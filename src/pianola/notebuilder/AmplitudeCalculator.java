package pianola.notebuilder;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import sound.AmplitudeStateMap;
import sound.Wave;
import sound.SampleRate;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.ConcurrentHashMap;

class AmplitudeCalculator extends SampleCalculator {

    static PipeCallable<BoundedBuffer<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>>, BoundedBuffer<AmplitudeStateMap, OrderStampedPacket<AmplitudeStateMap>>> buildPipe(SampleRate sampleRate) {
        final Map<Frequency, Wave> liveWaves = new HashMap<>();
        final Map<Frequency, Long> waveTimeOuts = new HashMap<>();

        return new PipeCallable<>() {
            final ConcurrentHashMap<Long, Set<SimpleImmutableEntry<Frequency, Wave>>> unfinishedSampleFragments = new ConcurrentHashMap<>();

            @Override
            public BoundedBuffer<AmplitudeStateMap, OrderStampedPacket<AmplitudeStateMap>> call(BoundedBuffer<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>> inputBuffer) {
                return buildSampleCalculator(
                        inputBuffer,
                        unfinishedSampleFragments,
                        this::addNewNotes,
                        input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue()),
                        AmplitudeStateMap::add,
                        () -> new AmplitudeStateMap(new HashMap<>()),
                        "amplitude calculator");
            }

            private Long addNewNotes(NewNotesAmplitudeData newNotesAmplitudeData) {
                Long sampleCount = newNotesAmplitudeData.getSampleCount();

                Collection<Frequency> newNotes = newNotesAmplitudeData.getNewNotes();

                if(!newNotes.isEmpty()) {
                    removeDeadNotes(sampleCount);

                    Long endingSampleCount = newNotesAmplitudeData.getEndingSampleCount();
                    SimpleImmutableEntry<Map<Frequency, Wave>, Map<Frequency, Long>> recycledWavesWithTimeOuts = recycleWaves(endingSampleCount, newNotes);
                    Map<Frequency, Wave> recycledWaves = addRecycledWaves(endingSampleCount, recycledWavesWithTimeOuts.getKey(), recycledWavesWithTimeOuts.getValue());

                    HashSet<Frequency> missingNotes = new HashSet<>(newNotes);
                    missingNotes.removeAll(recycledWaves.keySet());
                    Map<Frequency, Wave> missingWaves = createWaves(endingSampleCount, missingNotes, sampleRate);
                    addWaves(sampleCount, endingSampleCount, missingWaves);
                }
                return sampleCount;
            }

            private void addWaves(Long sampleCount, Long endingSampleCount, Map<Frequency, Wave> missingWaves) {
                if(!missingWaves.isEmpty()) {
                    HashSet<SimpleImmutableEntry<Frequency, Wave>> missingWavesEntries = new HashSet<>();
                    Set<Map.Entry<Frequency, Wave>> entries = missingWaves.entrySet();
                    for(Map.Entry<Frequency, Wave> entry: entries){
                        missingWavesEntries.add(new SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
                    }

                    for (Long i = sampleCount; i <= endingSampleCount; i++) {
                        Set<SimpleImmutableEntry<Frequency, Wave>> unfinishedFragmentsForThisSample = unfinishedSampleFragments.get(i);
                        if(unfinishedFragmentsForThisSample!=null) {
                                unfinishedFragmentsForThisSample.addAll(missingWavesEntries);
                        } else {
                            unfinishedSampleFragments.put(i, Collections.synchronizedSet(new HashSet<>(missingWavesEntries)));
                        }
                    }
                }
            }

            private Map<Frequency, Wave> addRecycledWaves(Long endingSampleCount, Map<Frequency, Wave> recycledWaves, Map<Frequency, Long> recycledWaveTimeOuts) {
                for(Frequency frequency : recycledWaves.keySet()){
                    for (Long i = recycledWaveTimeOuts.get(frequency)+1; i <= endingSampleCount; i++) {
                        Wave wave = recycledWaves.get(frequency);
                        SimpleImmutableEntry<Frequency, Wave> entry = new SimpleImmutableEntry<>(frequency, wave);
                        Set<SimpleImmutableEntry<Frequency, Wave>> unfinishedFragmentsForThisSample = unfinishedSampleFragments.get(i);
                        if(unfinishedFragmentsForThisSample!=null) {
                            unfinishedFragmentsForThisSample.add(entry);
                        } else {
                            Set<SimpleImmutableEntry<Frequency, Wave>> newSet = Collections.synchronizedSet(new HashSet<>());
                            newSet.add(entry);
                            unfinishedSampleFragments.put(i, newSet);
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

            private SimpleImmutableEntry<Map<Frequency, Wave>, Map<Frequency, Long>> recycleWaves(Long endingSampleCount, Collection<Frequency> newNotes) {
                Map<Frequency, Wave> foundWaves = new HashMap<>(liveWaves);
                foundWaves.keySet().retainAll(newNotes);
                Map<Frequency, Long> foundTimeOuts = new HashMap<>(waveTimeOuts);
                foundTimeOuts.keySet().retainAll(newNotes);
                for(Frequency frequency : foundWaves.keySet()){
                    waveTimeOuts.put(frequency, endingSampleCount);
                }
                return new SimpleImmutableEntry<>(foundWaves, foundTimeOuts);
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


    private static AmplitudeStateMap calculateAmplitudesPerFrequency(Long sampleCount, Map.Entry<Frequency, Wave> wavesPerFrequency) {
        return new AmplitudeStateMap(Collections.singletonMap(wavesPerFrequency.getKey(), wavesPerFrequency.getValue().getAmplitude(sampleCount)));
    }
}
