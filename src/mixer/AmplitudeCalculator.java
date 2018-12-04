package mixer;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.Orderer;
import frequency.Frequency;
import mixer.state.AmplitudeState;
import mixer.state.Wave;
import sound.SampleRate;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.ConcurrentHashMap;

class AmplitudeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>>, BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>>> buildPipe(SampleRate sampleRate) {
        return new PipeCallable<>() {
            final ConcurrentHashMap<Long, Set<SimpleImmutableEntry<Frequency, Wave>>> unfinishedSampleFragments = new ConcurrentHashMap<>();
            final Map<Frequency, Wave> liveWaves = new HashMap<>();
            final Map<Frequency, Long> waveTimeOuts = new HashMap<>();

            @Override
            public BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>> call(BoundedBuffer<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>> inputBuffer) {
                SimpleImmutableEntry<BoundedBuffer<SimpleImmutableEntry<Long, Set<SimpleImmutableEntry<Frequency, Wave>>>, Packet<SimpleImmutableEntry<Long, Set<SimpleImmutableEntry<Frequency, Wave>>>>>, BoundedBuffer<Set<AmplitudeState>, Packet<Set<AmplitudeState>>>> precalculatorOutputs = inputBuffer
                        .connectTo(Orderer.buildPipe("amplitude calculator - order input"))
                        .performMethod(((PipeCallable<NewNotesAmplitudeData, Long>) this::addNewNotes).toSequential(), "amplitude calculator - add new notes")
                        .connectTo(MapPrecalculator.buildPipe(
                                unfinishedSampleFragments,
                                input2 -> calculateAmplitudesPerFrequency(input2.getKey(), input2.getValue())
                        ));

                return precalculatorOutputs.getValue()
                        .<AmplitudeState, OrderStampedPacket<AmplitudeState>>performMethod(input1 -> {
                                    AmplitudeState sum = new AmplitudeState(new HashMap<>());
                                    for (AmplitudeState finishedSampleFragment : input1) {
                                        sum = sum.add(finishedSampleFragment);
                                    }
                                    return sum;
                                },
                                "amplitude calculator - fold precalculated sample fragments")
                        .connectTo(Orderer.buildPipe("amplitude calculator - order folded precalculated sample fragments"))
                        .pairWith(
                                precalculatorOutputs.getKey()
                                        .<AmplitudeState, OrderStampedPacket<AmplitudeState>>performMethod(input2 -> {
                                                    AmplitudeState sum1 = new AmplitudeState(new HashMap<>());
                                                    Long sampleCount = input2.getKey();
                                                    for (Map.Entry<Frequency, Wave> unfinishedSampleFragment : input2.getValue()) {
                                                        sum1 = sum1.add(
                                                                calculateAmplitudesPerFrequency(
                                                                        sampleCount,
                                                                        unfinishedSampleFragment));
                                                    }
                                                    return sum1;
                                                },
                                                "amplitude calculator - finish fragment calculation")
                                .connectTo(Orderer.buildPipe("amplitude calculator - order finished fragment calculations")),
                                "amplitude calculator - pair calculated and precalculated sample fragments")
                        .performMethod(input -> input.getKey().add(input.getValue()), "amplitude calculator - construct finished sample");
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
                            unfinishedSampleFragments.put(i, new HashSet<>(missingWavesEntries));
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
                            HashSet<SimpleImmutableEntry<Frequency, Wave>> newSet = new HashSet<>();
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

    private static AmplitudeState calculateAmplitudesPerFrequency(Long sampleCount, Map.Entry<Frequency, Wave> wavesPerFrequency) {
        return new AmplitudeState(Collections.singletonMap(wavesPerFrequency.getKey(), wavesPerFrequency.getValue().getAmplitude(sampleCount)));
    }
}
