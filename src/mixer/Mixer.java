package mixer;

import component.Counter;
import component.Pulse;
import component.buffer.*;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.Envelope;
import mixer.state.*;
import sound.SampleRate;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Collectors;

public class Mixer extends MethodPipeComponent<Pulse, VolumeAmplitudeState> {

    public Mixer(SimpleBuffer<Pulse> sampleCountBuffer, BoundedBuffer<Frequency> noteInputBuffer, SimpleBuffer<VolumeAmplitudeState> outputBuffer, SampleRate sampleRate){
        super(sampleCountBuffer, outputBuffer, toMethod(buildPipe(noteInputBuffer, sampleRate)));
    }

    public static PipeCallable<BoundedBuffer<Pulse>, BoundedBuffer<VolumeAmplitudeState>> buildPipe(BoundedBuffer<Frequency> noteInputBuffer, SampleRate sampleRate){
        return new PipeCallable<>() {
            private BoundedBuffer<Pulse> inputBuffer;

            private OutputPort<Long> sampleCountOutputPort;

            private VolumeCalculator volumeCalculator;
            private AmplitudeCalculator amplitudeCalculator;

            @Override
            public BoundedBuffer<VolumeAmplitudeState> call(BoundedBuffer<Pulse> inputBuffer) {
                sampleCountOutputPort = new OutputPort<>("mixer - sample count");
                LinkedList<SimpleBuffer<Long>> sampleBroadcast = new LinkedList<>(sampleCountOutputPort.getBuffer().broadcast(2));

                volumeCalculator = new VolumeCalculator(sampleBroadcast.poll());
                amplitudeCalculator = new AmplitudeCalculator(sampleBroadcast.poll());

                OutputPort<TimestampedFrequencies> noteAdderInputPort = new OutputPort<>();
                SimpleBuffer<Long> mixInputBuffer = new SimpleBuffer<>(1, "mixer - mix input");
                OutputPort<Long> mixInput = mixInputBuffer.createOutputPort();

                //Precalculate
                this.inputBuffer = inputBuffer;
                inputBuffer
                .performMethod(input1 -> {
//                            precalculateInBackground();
                    return input1;
                })
                //Determine whether to add new notes or mix
                .performMethod(Counter.build(), "count samples")
                .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                .performInputMethod(((InputCallable<TimestampedFrequencies>) timestampedFrequencies -> {
                    if (timestampedFrequencies.getFrequencies().isEmpty()) {
                        try {
                            mixInput.produce(timestampedFrequencies.getSampleCount());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            noteAdderInputPort.produce(timestampedFrequencies);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).toSequential());

                //Add new notes
                noteAdderInputPort.getBuffer()
                        .performMethod(EnvelopeBuilder.buildEnvelope(sampleRate), "build envelope wave")
                .performMethod(((PipeCallable<TimestampedNewNotesWithEnvelope, Long>)
                        this::addNewNotes)
                        .toSequential(), "mixer - add new notes")
                .relayTo(mixInputBuffer);

                //Mix
                return mixInputBuffer
                        .performMethod(((PipeCallable<Long, VolumeAmplitudeState>)
                                this::mix)
                                .toSequential(), "mixer - mix");
            }

            private void precalculateInBackground() {
                while (inputBuffer.isEmpty()) {
                    try {
                        Long futureSampleCount = volumeCalculator.unfinishedEnvelopeSlices.keySet().iterator().next();

                        SimpleImmutableEntry<VolumeState, AmplitudeState> volumeAmplitudeState = calculateVolumeAmplitude(futureSampleCount);

                        volumeCalculator.finishedVolumeSlices.put(futureSampleCount, volumeAmplitudeState.getKey());
                        amplitudeCalculator.finishedAmplitudeSlices.put(futureSampleCount, volumeAmplitudeState.getValue());
                    } catch (NoSuchElementException e) {
                        break;
                    }
                    catch (NullPointerException ignored){
                        //Before the volume calculator and amplitude calculator have been initialized,
                        //we throw a NullPointerException.
                    }
                }
            }

            private SimpleImmutableEntry<VolumeState, AmplitudeState> calculateVolumeAmplitude(Long sampleCount) {
                try {
                    sampleCountOutputPort.produce(sampleCount);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                VolumeState finishedVolumeSlice = volumeCalculator.calculateVolume(sampleCount);
                AmplitudeState finishedAmplitudeSlice = amplitudeCalculator.calculateAmplitude(sampleCount);

                return new SimpleImmutableEntry<>(finishedVolumeSlice, finishedAmplitudeSlice);
            }

            private VolumeAmplitudeState mix(Long sampleCount) {
                SimpleImmutableEntry<VolumeState, AmplitudeState> volumeAmplitudeState = calculateVolumeAmplitude(sampleCount);

                return new VolumeAmplitudeState(volumeAmplitudeState.getKey(), volumeAmplitudeState.getValue());
            }

            private Long addNewNotes(TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope) {
                Long startingSampleCount = timestampedNewNotesWithEnvelope.getSampleCount();
                Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();
                DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
                long endingSampleCount = envelope.getEndingSampleCount();

                volumeCalculator.addNewEnvelopes(startingSampleCount, endingSampleCount, newNotes, envelope);

                amplitudeCalculator.addNewWaves(startingSampleCount, endingSampleCount, newNotes, sampleRate);

                return startingSampleCount;
            }

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }

    private static Map<Frequency, Double> sumValuesPerFrequency(Map<Frequency, Collection<Double>> collectionMap) {
        Map<Frequency, Double> totalMap = new HashMap<>();
        for(Frequency frequency : collectionMap.keySet()) {
            Collection<Double> collection = collectionMap.get(frequency);
            Double total = collection.stream().mapToDouble(f -> f).sum();
            totalMap.put(frequency, total);
        }
        return totalMap;
    }


    private static Collection<EnvelopeForFrequency> distribute(DeterministicEnvelope envelope, Collection<Frequency> frequencies) {
        Collection<EnvelopeForFrequency> newNotesWithEnvelopes = new LinkedList<>();
        for(Frequency frequency : frequencies){
            newNotesWithEnvelopes.add(new EnvelopeForFrequency(frequency, envelope));
        }
        return newNotesWithEnvelopes;
    }

    private static class VolumeCalculator {
        final Map<Long, Collection<EnvelopeForFrequency>> unfinishedEnvelopeSlices;
        final Map<Long, VolumeState> finishedVolumeSlices;
        final OutputPort<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyOutputPort;
        final OutputPort<VolumeState> oldVolumeStateOutputPort;
        final InputPort<VolumeState> newVolumeStateInputPort;

        public VolumeCalculator(SimpleBuffer<Long> sampleBuffer){
            unfinishedEnvelopeSlices = new HashMap<>();
            finishedVolumeSlices = new HashMap<>();

            groupEnvelopesByFrequencyOutputPort = new OutputPort<>("mixer - group envelopes by frequency");
            oldVolumeStateOutputPort = new OutputPort<>("mixer - old volume state");
            newVolumeStateInputPort =
                    oldVolumeStateOutputPort.getBuffer()
                            .pairWith(
                                    sampleBuffer
                                            .pairWith(
                                                    groupEnvelopesByFrequencyOutputPort.getBuffer()
                                                            .performMethod(((PipeCallable<Collection<EnvelopeForFrequency>, Map<Frequency, Collection<Envelope>>>)
                                                                    VolumeCalculator::groupEnvelopesByFrequency)
                                                                    .toSequential(), "group envelopes by frequency precalc"))
                                            .performMethod(input -> calculateVolumesPerFrequency(input.getKey(), input.getValue()))
                                            .performMethod(Mixer::sumValuesPerFrequency)
                                            .performMethod(VolumeState::new))
                            .performMethod(
                                    input1 ->
                                            input1.getKey()
                                                    .add(input1.getValue()))
                            .createInputPort();
        }

        private static Map<Frequency, Collection<Envelope>> groupEnvelopesByFrequency(Collection<EnvelopeForFrequency> envelopesForFrequencies) {
            Map<Frequency, List<EnvelopeForFrequency>> groupedEnvelopeFroFrequencies = envelopesForFrequencies.stream().collect(Collectors.groupingBy(EnvelopeForFrequency::getFrequency));
            return
                groupedEnvelopeFroFrequencies.entrySet()
                .stream()
                .collect(
                    Collectors.toMap(Map.Entry::getKey,
                    e ->
                        e.getValue()
                        .stream()
                        .map(EnvelopeForFrequency::getEnvelope)
                        .collect(Collectors.toList())));
        }

        private static Map<Frequency, Collection<Double>> calculateVolumesPerFrequency(Long sampleCount, Map<Frequency, Collection<Envelope>> envelopesPerFrequency) {
            Map<Frequency, Collection<Double>> newVolumeCollections = new HashMap<>();
            for (Frequency frequency : envelopesPerFrequency.keySet()) {
                Collection<Envelope> envelopes = envelopesPerFrequency.get(frequency);
                try {
                    Collection<Double> volumes = new LinkedList<>();
                    for (Envelope envelope : envelopes) {
                        double volume = envelope.getVolume(sampleCount);
                        volumes.add(volume);
                    }

                    newVolumeCollections.put(frequency, volumes);
                }
                catch(NullPointerException ignored){
                }
            }
            return newVolumeCollections;
        }

        private void addNewEnvelopes(Long sampleCount, Long endingSampleCount, Collection<Frequency> newNotes, DeterministicEnvelope envelope) {
            Collection<EnvelopeForFrequency> newNotesWithEnvelopes = distribute(
                    envelope,
                    newNotes);

            for (Long i = sampleCount; i < endingSampleCount; i++) {
                Collection<EnvelopeForFrequency> newUnfinishedSlice = unfinishedEnvelopeSlices.remove(i);
                try {
                    newUnfinishedSlice.addAll(newNotesWithEnvelopes);
                } catch (NullPointerException e) {
                    newUnfinishedSlice = new LinkedList<>(newNotesWithEnvelopes);
                }
                unfinishedEnvelopeSlices.put(i, newUnfinishedSlice);
            }
        }

        private VolumeState calculateVolume(Long sampleCount) {
            Collection<EnvelopeForFrequency> currentUnfinishedSlice = unfinishedEnvelopeSlices.remove(sampleCount);
            VolumeState oldFinishedVolumeSlice = finishedVolumeSlices.remove(sampleCount);

            try {
                try {
                    groupEnvelopesByFrequencyOutputPort.produce(currentUnfinishedSlice);
                } catch (NullPointerException e) {
                    groupEnvelopesByFrequencyOutputPort.produce(new LinkedList<>());
                }

                try {
                    oldVolumeStateOutputPort.produce(oldFinishedVolumeSlice);
                } catch (NullPointerException e) {
                    oldVolumeStateOutputPort.produce(new VolumeState(new HashMap<>()));
                }

                return newVolumeStateInputPort.consume();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static class AmplitudeCalculator {
        final Map<Long, Map<Frequency, Wave>> unfinishedWaveSlices;
        final Map<Long, AmplitudeState> finishedAmplitudeSlices;
        final OutputPort<Map<Frequency, Wave>> waveOutputPort;
        final OutputPort<AmplitudeState> oldAmplitudeStateOutputPort;
        final InputPort<AmplitudeState> newAmplitudeStateInputPort;

        public AmplitudeCalculator(SimpleBuffer<Long> sampleBuffer) {
            unfinishedWaveSlices = new HashMap<>();
            finishedAmplitudeSlices = new HashMap<>();

            waveOutputPort = new OutputPort<>("mixer - wave output");
            oldAmplitudeStateOutputPort = new OutputPort<>("mixer - old amplitude state");
            newAmplitudeStateInputPort =
                    oldAmplitudeStateOutputPort.getBuffer()
                            .pairWith(
                                    sampleBuffer
                                            .pairWith(
                                                    waveOutputPort.getBuffer())
                                            .performMethod(input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue()))
                                            .performMethod(AmplitudeState::new))
                            .performMethod(
                                    input1 ->
                                            input1.getKey()
                                                    .add(input1.getValue()))
                            .createInputPort();

        }

        private static Map<Frequency, Double> calculateAmplitudesPerFrequency(Long sampleCount, Map<Frequency, Wave> wavesPerFrequency) {
            Map<Frequency, Double> newAmplitudeCollections = new HashMap<>();
            for (Frequency frequency : wavesPerFrequency.keySet()) {
                Wave wave = wavesPerFrequency.get(frequency);
                try {
                    double amplitude = wave.getAmplitude(sampleCount);
                    newAmplitudeCollections.put(frequency, amplitude);
                }
                catch(NullPointerException ignored){
                }
            }
            return newAmplitudeCollections;
        }

        private AmplitudeState calculateAmplitude(Long sampleCount) {
            Map<Frequency, Wave> currentUnfinishedWaveSlice;
            synchronized (unfinishedWaveSlices) {
                currentUnfinishedWaveSlice = unfinishedWaveSlices.remove(sampleCount);
            }

            AmplitudeState oldFinishedAmplitudeSlice = finishedAmplitudeSlices.remove(sampleCount);

            try {
                try {
                    waveOutputPort.produce(currentUnfinishedWaveSlice);
                } catch (NullPointerException e) {
                    waveOutputPort.produce(new HashMap<>());
                }

                try {
                    oldAmplitudeStateOutputPort.produce(oldFinishedAmplitudeSlice);
                } catch (NullPointerException e) {
                    oldAmplitudeStateOutputPort.produce(new AmplitudeState(new HashMap<>()));
                }

                return newAmplitudeStateInputPort.consume();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void addNewWaves(Long sampleCount, Long endingSampleCount, Collection<Frequency> newNotes, SampleRate sampleRate) {
            Map<Frequency, Wave> newNoteWaves = reuseOrCreateNewWaves(newNotes, sampleRate);

            for (Long i = sampleCount; i < endingSampleCount; i++) {
                synchronized (unfinishedWaveSlices) {
                    Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedWaveSlices.get(i);
                    try {
                        Map<Frequency, Wave> missingNoteWaves = new HashMap<>(newNoteWaves);
                        missingNoteWaves.keySet().removeAll(oldUnfinishedSliceWaves.keySet());
                        oldUnfinishedSliceWaves.putAll(missingNoteWaves);
                    } catch (NullPointerException e) {
                        Map<Frequency, Wave> newUnfinishedSliceWaves = new HashMap<>(newNoteWaves);
                        unfinishedWaveSlices.put(i, newUnfinishedSliceWaves);
                    }
                }
            }
        }

        private Map<Frequency, Wave> reuseOrCreateNewWaves(Collection<Frequency> newNotes, SampleRate sampleRate) {
            Map<Frequency, Wave> newNoteWaves = new HashMap<>();
            Set<Frequency> missingWaveFrequencies = new HashSet<>(newNotes);
            synchronized (unfinishedWaveSlices) {
                for (Long i : unfinishedWaveSlices.keySet()) {
                    Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedWaveSlices.get(i);

                    Map<Frequency, Wave> foundWaves = new HashMap<>(oldUnfinishedSliceWaves);
                    foundWaves.keySet().retainAll(missingWaveFrequencies);
                    newNoteWaves.putAll(foundWaves);

                    missingWaveFrequencies = new HashSet<>(missingWaveFrequencies);
                    missingWaveFrequencies.removeAll(oldUnfinishedSliceWaves.keySet());
                }
            }
            for (Frequency frequency : missingWaveFrequencies) {
                Wave newWave = new Wave(frequency, sampleRate);
                newNoteWaves.put(frequency, newWave);
            }
            return newNoteWaves;
        }
    }
}
