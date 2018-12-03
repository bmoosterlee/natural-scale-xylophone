package mixer;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import mixer.state.AmplitudeState;
import mixer.state.Wave;
import sound.SampleRate;

import java.util.*;

class AmplitudeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>>, BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>>> buildPipe(SampleRate sampleRate) {
        return new PipeCallable<>() {
            final Map<Long, Map<Frequency, Wave>> unfinishedSampleFragments = new HashMap<>();
            final Map<Frequency, Wave> liveWaves = new HashMap<>();
            final Map<Frequency, Long> waveTimeOuts = new HashMap<>();

            @Override
            public BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>> call(BoundedBuffer<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>> inputBuffer) {
                return inputBuffer
                        .performMethod(((PipeCallable<NewNotesAmplitudeData, Long>) this::addNewWaves).toSequential(), "amplitude calculator - add new notes")
                        .<PrecalculatorOutputData<Long, Map<Frequency, Wave>, AmplitudeState>, OrderStampedPacket<PrecalculatorOutputData<Long, Map<Frequency, Wave>, AmplitudeState>>>connectTo(MapPrecalculator.buildPipe(
                                unfinishedSampleFragments,
                                input2 -> calculateAmplitudesPerFrequency(input2.getKey(), input2.getValue()),
                                AmplitudeState::add,
                                HashMap::new,
                                () -> new AmplitudeState(new HashMap<>()))).performMethod(input -> input.getFinishedDataUntilNow()
                                .add(calculateAmplitudesPerFrequency(
                                        input.getIndex(),
                                        input.getFinalUnfinishedData())));
            }

            private Long addNewWaves(NewNotesAmplitudeData newNotesAmplitudeData) {
                Long sampleCount = newNotesAmplitudeData.getSampleCount();

                Collection<Frequency> newNotes = newNotesAmplitudeData.getNewNotes();

                if(!newNotes.isEmpty()) {
                    removeDeadNotes(sampleCount);

                    Long endingSampleCount = newNotesAmplitudeData.getEndingSampleCount();
                    AbstractMap.SimpleImmutableEntry<Map<Frequency, Wave>, Map<Frequency, Long>> recycledWavesWithTimeOuts = recycleWaves(endingSampleCount, newNotes);
                    Map<Frequency, Wave> recycledWaves = addRecycledWaves(endingSampleCount, recycledWavesWithTimeOuts.getKey(), recycledWavesWithTimeOuts.getValue());

                    HashSet<Frequency> missingNotes = new HashSet<>(newNotes);
                    missingNotes.removeAll(recycledWaves.keySet());
                    Map<Frequency, Wave> missingWaves = createWaves(endingSampleCount, missingNotes, sampleRate);
                    addWaves(sampleCount, endingSampleCount, missingWaves);
                }
                return sampleCount;
            }

            private void addWaves(Long sampleCount, Long endingSampleCount, Map<Frequency, Wave> missingWaves) {
                for (Long i = sampleCount; i <= endingSampleCount; i++) {
                    synchronized (unfinishedSampleFragments) {
                        Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSampleFragments.remove(i);
                        if (oldUnfinishedSliceWaves != null) {
                            Map<Frequency, Wave> missingNoteWaves = new HashMap<>(missingWaves);
                            missingNoteWaves.keySet().removeAll(oldUnfinishedSliceWaves.keySet());
                            oldUnfinishedSliceWaves.putAll(missingNoteWaves);
                        } else {
                            oldUnfinishedSliceWaves = new HashMap<>(missingWaves);
                        }
                        unfinishedSampleFragments.put(i, oldUnfinishedSliceWaves);
                    }
                }
            }

            private Map<Frequency, Wave> addRecycledWaves(Long endingSampleCount, Map<Frequency, Wave> recycledWaves, Map<Frequency, Long> recycledWaveTimeOuts) {
                for(Frequency frequency : recycledWaves.keySet()){
                    for (Long i = recycledWaveTimeOuts.get(frequency)+1; i <= endingSampleCount; i++) {
                        synchronized (unfinishedSampleFragments) {
                            Map<Frequency, Wave> unfinishedSampleFragment = unfinishedSampleFragments.remove(i);
                            Wave wave = recycledWaves.get(frequency);
                            if (unfinishedSampleFragment != null) {
                                unfinishedSampleFragment.put(frequency, wave);
                            } else {
                                unfinishedSampleFragment = new HashMap<>(Collections.singletonMap(frequency, wave));
                            }
                            unfinishedSampleFragments.put(i, unfinishedSampleFragment);
                        }
                    }
                }
                return recycledWaves;
            }

            private Map<Frequency, Wave> createWaves(Long endingSampleCount, Set<Frequency> missingNotes, SampleRate sampleRate) {
                Map<Frequency, Wave> createdWaves = new HashMap<>();
                for (Frequency frequency : missingNotes) {
                    Wave newWave = new Wave(frequency, sampleRate);
                    createdWaves.put(frequency, newWave);
                }
                liveWaves.putAll(createdWaves);
                for(Frequency frequency : createdWaves.keySet()){
                    waveTimeOuts.put(frequency, endingSampleCount);
                }
                return createdWaves;
            }

            private AbstractMap.SimpleImmutableEntry<Map<Frequency, Wave>, Map<Frequency, Long>> recycleWaves(Long endingSampleCount, Collection<Frequency> newNotes) {
                Map<Frequency, Wave> foundWaves = new HashMap<>(liveWaves);
                foundWaves.keySet().retainAll(newNotes);
                Map<Frequency, Long> foundTimeOuts = new HashMap<>(waveTimeOuts);
                foundTimeOuts.keySet().retainAll(newNotes);
                for(Frequency frequency : foundWaves.keySet()){
                    waveTimeOuts.put(frequency, endingSampleCount);
                }
                return new AbstractMap.SimpleImmutableEntry<>(foundWaves, foundTimeOuts);
            }

            private void removeDeadNotes(Long sampleCount) {
                HashSet<Frequency> deadFrequencies = new HashSet<>();
                for(Frequency frequency : waveTimeOuts.keySet()){
                    if(waveTimeOuts.get(frequency)<sampleCount){
                        deadFrequencies.add(frequency);
                    }
                }
                waveTimeOuts.keySet().removeAll(deadFrequencies);
                liveWaves.keySet().removeAll(deadFrequencies);
            }
        };
    }

    private static AmplitudeState calculateAmplitudesPerFrequency(Long sampleCount, Map<Frequency, Wave> wavesPerFrequency) {
        Map<Frequency, Double> newAmplitudeCollections = new HashMap<>();
        for (Frequency frequency : wavesPerFrequency.keySet()) {
            Wave wave = wavesPerFrequency.get(frequency);
            double amplitude = wave.getAmplitude(sampleCount);
            newAmplitudeCollections.put(frequency, amplitude);
        }
        return new AmplitudeState(newAmplitudeCollections);
    }
}
