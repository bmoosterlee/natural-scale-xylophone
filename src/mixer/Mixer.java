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

            private PipeCallable<Long, VolumeState> calculateVolume;
            private PipeCallable<Long, AmplitudeState> calculateAmplitude;

            @Override
            public SimpleImmutableEntry<BoundedBuffer<VolumeState>, BoundedBuffer<AmplitudeState>> call(){
                volumeCalculator = new VolumeCalculator();
                amplitudeCalculator = new AmplitudeCalculator();

                calculateVolume = MethodPipeComponent.toMethod(volumeCalculator.buildPipe());
                calculateAmplitude = MethodPipeComponent.toMethod(amplitudeCalculator.buildPipe());

                //Mix
                LinkedList<SimpleBuffer<Long>> mixInputBroadcast = new LinkedList<>(
                        inputBuffer
                        //Precalculate future samples
                        .performMethod(((PipeCallable<Pulse, Pulse>) input1 -> {
//                            precalculateInBackground();
                            return input1;
                        }).toSequential(), "mixer - precalculate")
                        //Determine whether to add new notes or mix
                        .performMethod(Counter.build(), "count samples")
//                        .performMethod(OrderStamper.build(), "mixer - order stamp")
                        .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                        .performMethod(EnvelopeBuilder.buildEnvelope(sampleRate), "build envelope wave")
                        .performMethod(((PipeCallable<TimestampedNewNotesWithEnvelope, Long>) this::addNewNotes)
                        .toSequential(), "mixer - add new notes")
                        .broadcast(2, "mixer - input broadcast"));

                return new SimpleImmutableEntry<>(
                        mixInputBroadcast.poll()
                        .connectTo(volumeCalculator.buildPipe().toSequential()),
                        mixInputBroadcast.poll()
                        .connectTo(amplitudeCalculator.buildPipe().toSequential()));
            }

            private void precalculateInBackground() {
                while (inputBuffer.isEmpty()) {
                    try {
                        Long futureSampleCount = volumeCalculator.unfinishedEnvelopeSlices.keySet().iterator().next();

                        VolumeState finishedVolumeSlice = calculateVolume.call(futureSampleCount);
                        AmplitudeState finishedAmplitudeSlice = calculateAmplitude.call(futureSampleCount);

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
                if(!newNotes.isEmpty()){
                    DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
                    long endingSampleCount = envelope.getEndingSampleCount();

                    volumeCalculator.addNewEnvelopes(startingSampleCount, endingSampleCount, newNotes, envelope);

                    amplitudeCalculator.addNewWaves(startingSampleCount, endingSampleCount, newNotes, sampleRate);
                }
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

        public VolumeCalculator(){
            unfinishedEnvelopeSlices = new HashMap<>();
            finishedVolumeSlices = new HashMap<>();
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

        private static Map<Frequency, Double> sumValuesPerFrequency(Map<Frequency, Collection<Double>> collectionMap) {
            Map<Frequency, Double> totalMap = new HashMap<>();
            for(Frequency frequency : collectionMap.keySet()) {
                Collection<Double> collection = collectionMap.get(frequency);
                Double total = collection.stream().mapToDouble(f -> f).sum();
                totalMap.put(frequency, total);
            }
            return totalMap;
        }

        private PipeCallable<BoundedBuffer<Long>, BoundedBuffer<VolumeState>> buildPipe() {
            return inputBuffer -> {
                LinkedList<SimpleBuffer<Long>> sampleCountBroadcast = new LinkedList<>(inputBuffer.broadcast(3, "volume calculator - sample broadcast"));
                return sampleCountBroadcast.poll()
                        .performMethod(((PipeCallable<Long, VolumeState>) VolumeCalculator.this::removeFinishedSliceForCalculation).toSequential(), "volume calculator - remove finished slice")
                        .pairWith(
                            sampleCountBroadcast.poll()
                            .pairWith(
                                    sampleCountBroadcast.poll()
                                    .performMethod(((PipeCallable<Long, Collection<EnvelopeForFrequency>>) VolumeCalculator.this::removeUnfinishedSliceForCalculation).toSequential(), "volume calculator - remove unfinished slice")
                                    .performMethod(((PipeCallable<Collection<EnvelopeForFrequency>, Map<Frequency, Collection<Envelope>>>) VolumeCalculator::groupEnvelopesByFrequency).toSequential(), "volume calculator - group envelopes by frequency"), "volume calculator - pair sample count and envelopes grouped by frequency")
                            .performMethod(((PipeCallable<SimpleImmutableEntry<Long, Map<Frequency, Collection<Envelope>>>, Map<Frequency, Collection<Double>>>) input -> calculateVolumesPerFrequency(input.getKey(), input.getValue())).toSequential(), "volume calculator - calculate volumes per frequency")
                            .performMethod(((PipeCallable<Map<Frequency, Collection<Double>>, Map<Frequency, Double>>) VolumeCalculator::sumValuesPerFrequency).toSequential(), "volume calculator - sum values per frequency")
                            .performMethod(((PipeCallable<Map<Frequency, Double>, VolumeState>) VolumeState::new).toSequential(), "volume calculator - construct new volume state"), "volume calculator - pair old and new finished slices")
                        .performMethod(
                                ((PipeCallable<SimpleImmutableEntry<VolumeState, VolumeState>, VolumeState>) input1 ->
                                        input1.getKey()
                                                .add(input1.getValue()))
                                        .toSequential(), "volume calculator - add old and new finished slices");
            };
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

        public AmplitudeCalculator() {
            unfinishedWaveSlices = new HashMap<>();
            finishedAmplitudeSlices = new HashMap<>();
        }

        private PipeCallable<BoundedBuffer<Long>, BoundedBuffer<AmplitudeState>> buildPipe() {
            return inputBuffer -> {
                LinkedList<SimpleBuffer<Long>> sampleCountBroadcast = new LinkedList<>(inputBuffer.broadcast(3, "amplitude calculator - sample broadcast"));

                return sampleCountBroadcast.poll()
                        .performMethod(((PipeCallable<Long, AmplitudeState>) AmplitudeCalculator.this::removeOldFinishedSliceForCalculation).toSequential(), "amplitude calculator - remove finished slice")
                        .pairWith(
                                sampleCountBroadcast.poll()
                                        .pairWith(
                                                sampleCountBroadcast.poll()
                                                .performMethod(((PipeCallable<Long, Map<Frequency, Wave>>) AmplitudeCalculator.this::removeUnfinishedSliceForCalculation).toSequential(), "amplitude calculator - remove unfinished slice"), "amplitude calculator - pair sample count and unfinished slice")
                                        .performMethod(((PipeCallable<SimpleImmutableEntry<Long, Map<Frequency, Wave>>, Map<Frequency, Double>>) input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue())).toSequential(), "amplitude calculator - calculate amplitudes per frequency")
                                        .performMethod(((PipeCallable<Map<Frequency, Double>, AmplitudeState>) AmplitudeState::new).toSequential(), "amplitude calculator - construct new amplitude state"), "amplitude calculator - pair new and old finished slices")
                        .performMethod(
                                ((PipeCallable<SimpleImmutableEntry<AmplitudeState, AmplitudeState>, AmplitudeState>) input1 ->
                                        input1.getKey()
                                                .add(input1.getValue()))
                                        .toSequential(), "amplitude calculator - add new and old finished slices");
            };
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
