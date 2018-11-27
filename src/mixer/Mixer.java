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
            private Map<Long, Collection<EnvelopeForFrequency>> unfinishedEnvelopeSlices;
            private final Map<Long, Map<Frequency, Wave>> unfinishedWaveSlices;
            private Map<Long, VolumeState> finishedVolumeSlices;
            private Map<Long, AmplitudeState> finishedAmplitudeSlices;

            private InputPort<TimestampedNewNotesWithEnvelope> timestampedNewNotesWithEnvelopeInputPort;

            private OutputPort<SimpleImmutableEntry<DeterministicEnvelope, Collection<Frequency>>> addNewNotesOutputPort;
            private InputPort<Collection<EnvelopeForFrequency>> addNewNotesInputPort;

            private OutputPort<Long> sampleCountOutputPort;
            private OutputPort<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyOutputPort;
            private OutputPort<Map<Frequency, Wave>> waveOutputPort;
            private OutputPort<VolumeState> oldVolumeStateOutputPort;
            private OutputPort<AmplitudeState> oldAmplitudeStateOutputPort;
            private InputPort<VolumeState> newVolumeStateInputPort;
            private InputPort<AmplitudeState> newAmplitudeStateInputPort;

            {
                unfinishedEnvelopeSlices = new HashMap<>();
                unfinishedWaveSlices = new HashMap<>();
                finishedVolumeSlices = new HashMap<>();
                finishedAmplitudeSlices = new HashMap<>();
            }

            @Override
            public BoundedBuffer<VolumeAmplitudeState> call(BoundedBuffer<Pulse> inputBuffer) {
                LinkedList<SimpleBuffer<Long>> inputBroadcast =
                    new LinkedList<>(
                        inputBuffer
                        .performMethod(
                            Counter.build(), "count samples")
                        .broadcast(2, "mixer - sample count"));

                timestampedNewNotesWithEnvelopeInputPort =
                        inputBroadcast.poll()
                                .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                                .performMethod(EnvelopeWaveBuilder.buildEnvelopeWave(sampleRate), "build envelope wave").createInputPort();

                addNewNotesOutputPort = new OutputPort<>();
                addNewNotesInputPort =
                    addNewNotesOutputPort.getBuffer()
                    .performMethod(
                            ((PipeCallable<SimpleImmutableEntry<DeterministicEnvelope, Collection<Frequency>>, Collection<EnvelopeForFrequency>>)
                                input -> toEnvelopesForFrequencies(
                                    input.getKey(),
                                    input.getValue()))
                            .toSequential(), "addNewNotes")
                    .createInputPort();

                BoundedBuffer<VolumeAmplitudeState> outputBuffer =
                        inputBroadcast.poll()
                        .performMethod(((PipeCallable<Long, VolumeAmplitudeState>)
                                this::mix)
                        .toSequential(), "mixer");

                sampleCountOutputPort = new OutputPort<>("mixer - sample count");
                groupEnvelopesByFrequencyOutputPort = new OutputPort<>("mixer - group envelopes by frequency");
                waveOutputPort = new OutputPort<>("mixer - wave output");
                oldVolumeStateOutputPort = new OutputPort<>("mixer - old volume state");
                oldAmplitudeStateOutputPort = new OutputPort<>("mixer - old amplitude state");

                LinkedList<SimpleBuffer<Long>> sampleBroadcast = new LinkedList<>(sampleCountOutputPort.getBuffer().broadcast(2));

                newVolumeStateInputPort =
                        oldVolumeStateOutputPort.getBuffer()
                        .pairWith(
                                sampleBroadcast.poll()
                                        .pairWith(
                                                groupEnvelopesByFrequencyOutputPort.getBuffer()
                                                        .performMethod(((PipeCallable<Collection<EnvelopeForFrequency>, Map<Frequency, Collection<Envelope>>>)
                                                                Mixer::groupEnvelopesByFrequency)
                                                                .toSequential(), "group envelopes by frequency precalc"))
                                        .performMethod(input -> calculateVolumesPerFrequency(input.getKey(), input.getValue()))
                                        .performMethod(Mixer::sumValuesPerFrequency)
                                        .performMethod(VolumeState::new))
                        .performMethod(
                            input1 ->
                                input1.getKey()
                                .add(input1.getValue()))
                .createInputPort();

                newAmplitudeStateInputPort =
                        oldAmplitudeStateOutputPort.getBuffer()
                        .pairWith(
                                sampleBroadcast.poll()
                                        .pairWith(
                                                waveOutputPort.getBuffer())
                                        .performMethod(input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue()))
                                        .performMethod(AmplitudeState::new))
                        .performMethod(
                            input1 ->
                                input1.getKey()
                                .add(input1.getValue()))
                    .createInputPort();

                return outputBuffer;
            }

            private void precalculateInBackground() {
                while (timestampedNewNotesWithEnvelopeInputPort.isEmpty()) {
                    try {
                        Long futureSampleCount = unfinishedEnvelopeSlices.keySet().iterator().next();
                        Collection<EnvelopeForFrequency> currentUnfinishedEnvelopeSlice = unfinishedEnvelopeSlices.remove(futureSampleCount);
                        Map<Frequency, Wave> currentUnfinishedSliceWaves;
                        synchronized (unfinishedWaveSlices) {
                            currentUnfinishedSliceWaves = unfinishedWaveSlices.remove(futureSampleCount);
                        }

                        try {
                            sampleCountOutputPort.produce(futureSampleCount);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        VolumeState finishedVolumeSlice = calculateVolume(currentUnfinishedEnvelopeSlice, finishedVolumeSlices.remove(futureSampleCount));
                        AmplitudeState finishedAmplitudeSlice = calculateAmplitude(currentUnfinishedSliceWaves, finishedAmplitudeSlices.remove(futureSampleCount));
                        finishedVolumeSlices.put(futureSampleCount, finishedVolumeSlice);
                        finishedAmplitudeSlices.put(futureSampleCount, finishedAmplitudeSlice);
                    } catch (NoSuchElementException e) {
                        break;
                    }
                }
            }

            private VolumeState calculateVolume(Collection<EnvelopeForFrequency> currentUnfinishedSlice, VolumeState oldFinishedVolumeSlice) {
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

            private AmplitudeState calculateAmplitude(Map<Frequency, Wave> currentUnfinishedWaveSlice, AmplitudeState oldFinishedAmplitudeSlice) {
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

            //todo there might be duplicate frequencies added at a timestamp. Group by frequency as well.
            //todo combine unfinishedSlice and unfinishedSliceWaves into one object

            private void addNewNotes(Long sampleCount, DeterministicEnvelope envelope, Collection<Frequency> newNotes) {
                Collection<EnvelopeForFrequency> newNotesWithEnvelopes;
                try {
                    addNewNotesOutputPort.produce(new SimpleImmutableEntry<>(envelope, newNotes));
                    newNotesWithEnvelopes = addNewNotesInputPort.consume();

                    Map<Frequency, Wave> newNoteWaves = reuseOrCreateNewWaves(newNotes);

                    long endingSampleCount = envelope.getEndingSampleCount();
                    addNewEnvelopes(sampleCount, endingSampleCount, newNotesWithEnvelopes);
                    addNewWaves(sampleCount, endingSampleCount, newNotes, newNoteWaves);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            private void addNewWaves(Long sampleCount, Long endingSampleCount, Collection<Frequency> newNotes, Map<Frequency, Wave> newNoteWaves) {
                if (newNotes.isEmpty()) {
                    return;
                }
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

            private void addNewEnvelopes(Long sampleCount, Long endingSampleCount, Collection<EnvelopeForFrequency> newNotesWithEnvelopes) {
                if (newNotesWithEnvelopes.isEmpty()) {
                    return;
                }
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

            private Map<Frequency, Wave> reuseOrCreateNewWaves(Collection<Frequency> newNotes) {
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

            private VolumeAmplitudeState mix(Long sampleCount) {
                try {
                    TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope = timestampedNewNotesWithEnvelopeInputPort.consume();
                    DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
                    Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();

                    addNewNotes(sampleCount, envelope, newNotes);

                    Collection<EnvelopeForFrequency> currentFinishedSlice = unfinishedEnvelopeSlices.remove(sampleCount);
                    Map<Frequency, Wave> currentUnfinishedSliceWaves;
                    synchronized (unfinishedWaveSlices) {
                        currentUnfinishedSliceWaves = unfinishedWaveSlices.remove(sampleCount);
                    }
                    sampleCountOutputPort.produce(sampleCount);
                    VolumeState finishedVolumeSlice = calculateVolume(currentFinishedSlice, finishedVolumeSlices.remove(sampleCount));
                    AmplitudeState finishedAmplitudeSlice = calculateAmplitude(currentUnfinishedSliceWaves, finishedAmplitudeSlices.remove(sampleCount));
                    VolumeAmplitudeState result = new VolumeAmplitudeState(finishedVolumeSlice, finishedAmplitudeSlice);

    //            precalculateInBackground();

                    return result;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
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


    private static Map<Frequency, Collection<Envelope>> groupEnvelopesByFrequency(Collection<EnvelopeForFrequency> envelopesForFrequencies) {
        Map<Frequency, List<EnvelopeForFrequency>> groupedEnvelopeFroFrequencies = envelopesForFrequencies.stream().collect(Collectors.groupingBy(w -> w.getFrequency()));
        Map<Frequency, Collection<Envelope>> result = groupedEnvelopeFroFrequencies.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream().map(EnvelopeForFrequency::getEnvelope).collect(Collectors.toList())));
        return result;
    }

    private static Collection<EnvelopeForFrequency> toEnvelopesForFrequencies(DeterministicEnvelope envelope, Collection<Frequency> frequencies) {
        Collection<EnvelopeForFrequency> newNotesWithEnvelopes = new LinkedList<>();
        for(Frequency frequency : frequencies){
            newNotesWithEnvelopes.add(new EnvelopeForFrequency(frequency, envelope));
        }
        return newNotesWithEnvelopes;
    }

}
