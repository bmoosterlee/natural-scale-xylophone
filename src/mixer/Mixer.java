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
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class Mixer {

    public static SimpleImmutableEntry<BoundedBuffer<VolumeState>, BoundedBuffer<AmplitudeState>> buildComponent(BoundedBuffer<Pulse> inputBuffer, BoundedBuffer<Frequency> noteInputBuffer, SampleRate sampleRate){
        Callable<SimpleImmutableEntry<BoundedBuffer<VolumeState>, BoundedBuffer<AmplitudeState>>> o = new Callable<>() {

            private VolumeCalculator volumeCalculator;
            private AmplitudeCalculator amplitudeCalculator;

            @Override
            public SimpleImmutableEntry<BoundedBuffer<VolumeState>, BoundedBuffer<AmplitudeState>> call(){
                volumeCalculator = new VolumeCalculator();
                amplitudeCalculator = new AmplitudeCalculator();

                OutputPort<TimestampedFrequencies> noteAdderInputPort = new OutputPort<>();
                SimpleBuffer<Long> mixInputBuffer = new SimpleBuffer<>(1, "mixer - mix input");
                OutputPort<Long> mixInput = mixInputBuffer.createOutputPort();

                //Precalculate
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
                LinkedList<SimpleBuffer<Long>> mixInputBroadcast = new LinkedList<>(mixInputBuffer.broadcast(2));
                return new SimpleImmutableEntry<>(
                        mixInputBroadcast.poll()
                                .performMethod(((PipeCallable<Long, VolumeState>)
                                        sampleCount -> volumeCalculator.calculateVolume(sampleCount))
                                        .toSequential(), "mixer - calculate volume"),
                        mixInputBroadcast.poll()
                                .performMethod(((PipeCallable<Long, AmplitudeState>)
                                        sampleCount -> amplitudeCalculator.calculateAmplitude(sampleCount))
                                        .toSequential(), "mixer - calculate amplitude"));
            }

            private void precalculateInBackground() {
                while (inputBuffer.isEmpty()) {
                    try {
                        Long futureSampleCount = volumeCalculator.unfinishedEnvelopeSlices.keySet().iterator().next();

                        VolumeState finishedVolumeSlice = volumeCalculator.calculateVolume(futureSampleCount);
                        AmplitudeState finishedAmplitudeSlice = amplitudeCalculator.calculateAmplitude(futureSampleCount);

                        volumeCalculator.finishedVolumeSlices.put(futureSampleCount, finishedVolumeSlice);
                        amplitudeCalculator.finishedAmplitudeSlices.put(futureSampleCount, finishedAmplitudeSlice);
                    } catch (NoSuchElementException e) {
                        break;
                    } catch (NullPointerException ignored) {
                        //Before the volume calculator and amplitude calculator have been initialized,
                        //we throw a NullPointerException.
                    }
                }
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
        };

        try {
            return o.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        private OutputPort<Long> sampleCountOutputPort;
        final InputPort<VolumeState> newVolumeStateInputPort;

        public VolumeCalculator(){
            unfinishedEnvelopeSlices = new HashMap<>();
            finishedVolumeSlices = new HashMap<>();

            sampleCountOutputPort = new OutputPort<>("volume calculator - sample count");
            LinkedList<SimpleBuffer<Long>> sampleCountBroadcast = new LinkedList<>(sampleCountOutputPort.getBuffer().broadcast(3));
            newVolumeStateInputPort =
                    sampleCountBroadcast.poll()
                    .performMethod(((PipeCallable<Long, VolumeState>) VolumeCalculator.this::removeFinishedSliceForCalculation).toSequential())
                            .pairWith(
                                    sampleCountBroadcast.poll()
                                            .pairWith(
                                                    sampleCountBroadcast.poll()
                                                    .performMethod(((PipeCallable<Long, Collection<EnvelopeForFrequency>>) VolumeCalculator.this::removeUnfinishedSliceForCalculation).toSequential())
                                                    .performMethod(((PipeCallable<Collection<EnvelopeForFrequency>, Map<Frequency, Collection<Envelope>>>) VolumeCalculator::groupEnvelopesByFrequency).toSequential()))
                                            .performMethod(((PipeCallable<SimpleImmutableEntry<Long, Map<Frequency, Collection<Envelope>>>, Map<Frequency, Collection<Double>>>) input -> calculateVolumesPerFrequency(input.getKey(), input.getValue())).toSequential())
                                            .performMethod(((PipeCallable<Map<Frequency, Collection<Double>>, Map<Frequency, Double>>) Mixer::sumValuesPerFrequency).toSequential())
                                            .performMethod(((PipeCallable<Map<Frequency, Double>, VolumeState>) VolumeState::new).toSequential()))
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
            try {
                sampleCountOutputPort.produce(sampleCount);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                return newVolumeStateInputPort.consume();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        private VolumeState removeFinishedSliceForCalculation(Long sampleCount) {
            VolumeState oldFinishedVolumeSlice = finishedVolumeSlices.remove(sampleCount);
            if(oldFinishedVolumeSlice==null){
                oldFinishedVolumeSlice = new VolumeState(new HashMap<>());
            }
            return oldFinishedVolumeSlice;
        }

        private Collection<EnvelopeForFrequency> removeUnfinishedSliceForCalculation(Long sampleCount) {
            Collection<EnvelopeForFrequency> currentUnfinishedSlice = unfinishedEnvelopeSlices.remove(sampleCount);
            if(currentUnfinishedSlice==null){
                currentUnfinishedSlice = new HashSet<>();
            }
            return currentUnfinishedSlice;
        }
    }

    private static class AmplitudeCalculator {
        final Map<Long, Map<Frequency, Wave>> unfinishedWaveSlices;
        final Map<Long, AmplitudeState> finishedAmplitudeSlices;
        private OutputPort<Long> sampleCountOutputPort;
        final InputPort<AmplitudeState> newAmplitudeStateInputPort;

        public AmplitudeCalculator() {
            unfinishedWaveSlices = new HashMap<>();
            finishedAmplitudeSlices = new HashMap<>();

            sampleCountOutputPort = new OutputPort<>("amplitude calculator - sample count");
            LinkedList<SimpleBuffer<Long>> sampleCountBroadcast = new LinkedList<>(sampleCountOutputPort.getBuffer().broadcast(3));

            newAmplitudeStateInputPort =
                    sampleCountBroadcast.poll().performMethod(((PipeCallable<Long, AmplitudeState>) AmplitudeCalculator.this::removeOldFinishedSliceForCalculation).toSequential())
                            .pairWith(
                                    sampleCountBroadcast.poll()
                                            .pairWith(
                                                    sampleCountBroadcast.poll().performMethod(((PipeCallable<Long, Map<Frequency, Wave>>) AmplitudeCalculator.this::removeUnfinishedSliceForCalculation).toSequential()))
                                            .performMethod(((PipeCallable<SimpleImmutableEntry<Long, Map<Frequency, Wave>>, Map<Frequency, Double>>) input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue())).toSequential())
                                            .performMethod(((PipeCallable<Map<Frequency, Double>, AmplitudeState>) AmplitudeState::new).toSequential()))
                            .performMethod(
                                    input1 ->
                                            input1.getKey()
                                            .add(input1.getValue()))
                            .createInputPort();

        }

        private AmplitudeState calculateAmplitude(Long sampleCount) {
            try {
                sampleCountOutputPort.produce(sampleCount);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try{
                return newAmplitudeStateInputPort.consume();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        private Map<Frequency, Wave> removeUnfinishedSliceForCalculation(Long sampleCount) {
            Map<Frequency, Wave> currentUnfinishedWaveSlice;
            synchronized (unfinishedWaveSlices) {
                currentUnfinishedWaveSlice = unfinishedWaveSlices.remove(sampleCount);
            }
            if(currentUnfinishedWaveSlice==null){
                currentUnfinishedWaveSlice = new HashMap<>();
            }
            return currentUnfinishedWaveSlice;
        }

        private AmplitudeState removeOldFinishedSliceForCalculation(Long sampleCount) {
            AmplitudeState oldFinishedAmplitudeSlice = finishedAmplitudeSlices.remove(sampleCount);
            if(oldFinishedAmplitudeSlice==null){
                oldFinishedAmplitudeSlice = new AmplitudeState(new HashMap<>());
            }
            return oldFinishedAmplitudeSlice;
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
