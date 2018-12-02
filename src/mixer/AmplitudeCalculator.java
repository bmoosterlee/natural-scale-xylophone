package mixer;

import component.buffer.*;
import frequency.Frequency;
import mixer.state.AmplitudeState;
import mixer.state.Wave;
import sound.SampleRate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

class AmplitudeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesAmplitudeData>, BoundedBuffer<AmplitudeState>> buildPipe(SampleRate sampleRate) {
        return new PipeCallable<>() {
            final Map<Long, Map<Frequency, Wave>> unfinishedSampleFragments = new ConcurrentHashMap<>();

            @Override
            public BoundedBuffer<AmplitudeState> call(BoundedBuffer<NewNotesAmplitudeData> inputBuffer) {
                LinkedList<SimpleBuffer<PrecalculatorOutputData<Long, Map<Frequency, Wave>, AmplitudeState>>> precalculatorOutputBroadcast =
                        new LinkedList<>(
                                inputBuffer
                                .performMethod(((PipeCallable<NewNotesAmplitudeData, Long>) this::addNewWaves).toSequential(), "amplitude calculator - add new notes")
                                .connectTo(MapPrecalculator.buildPipe(
                                        unfinishedSampleFragments,
                                        input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue()),
                                        AmplitudeState::add,
                                        HashMap::new,
                                        () -> new AmplitudeState(new HashMap<>())))
                                .broadcast(3, "precalculator output - broadcast"));

                return
                    precalculatorOutputBroadcast.poll()
                    .performMethod(((PipeCallable<PrecalculatorOutputData<Long, Map<Frequency, Wave>, AmplitudeState>, AmplitudeState>) PrecalculatorOutputData::getFinishedDataUntilNow).toSequential(), "amplitude calculator - remove finished slice")
                    .pairWith(
                            precalculatorOutputBroadcast.poll()
                            .performMethod(((PipeCallable<PrecalculatorOutputData<Long, Map<Frequency, Wave>, AmplitudeState>, Long>) PrecalculatorOutputData::getIndex).toSequential(), "amplitude calculator - extract sample count from precalculator")
                            .pairWith(
                                    precalculatorOutputBroadcast.poll()
                                    .performMethod(((PipeCallable<PrecalculatorOutputData<Long, Map<Frequency, Wave>, AmplitudeState>, Map<Frequency, Wave>>) PrecalculatorOutputData::getFinalUnfinishedData).toSequential(), "amplitude calculator - remove unfinished slice"), "amplitude calculator - pair sample count and unfinished slice")
                            .performMethod(((PipeCallable<AbstractMap.SimpleImmutableEntry<Long, Map<Frequency, Wave>>, AmplitudeState>) input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue())).toSequential(), "amplitude calculator - calculate amplitudes per frequency"), "amplitude calculator - pair new and old finished slices")
                    .performMethod(
                            ((PipeCallable<AbstractMap.SimpleImmutableEntry<AmplitudeState, AmplitudeState>, AmplitudeState>) input1 ->
                                    input1.getKey()
                                    .add(input1.getValue()))
                                    .toSequential(), "amplitude calculator - add new and old finished slices");
            }

            private Long addNewWaves(NewNotesAmplitudeData newNotesAmplitudeData) {
                Long sampleCount = newNotesAmplitudeData.getSampleCount();

                Map<Frequency, Wave> newNoteWaves = reuseOrCreateNewWaves(newNotesAmplitudeData.getNewNotes(), sampleRate);

                for (Long i = sampleCount; i < newNotesAmplitudeData.getEndingSampleCount(); i++) {
                    synchronized (unfinishedSampleFragments) {
                        Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSampleFragments.remove(i);
                        if(oldUnfinishedSliceWaves!=null){
                            Map<Frequency, Wave> missingNoteWaves = new HashMap<>(newNoteWaves);
                            missingNoteWaves.keySet().removeAll(oldUnfinishedSliceWaves.keySet());
                            oldUnfinishedSliceWaves.putAll(missingNoteWaves);
                        } else {
                            Map<Frequency, Wave> newUnfinishedSliceWaves = new HashMap<>(newNoteWaves);
                            unfinishedSampleFragments.put(i, newUnfinishedSliceWaves);
                        }
                    }
                }
                return sampleCount;
            }

            private Map<Frequency, Wave> reuseOrCreateNewWaves(Collection<Frequency> newNotes, SampleRate sampleRate) {
                Map<Frequency, Wave> newNoteWaves = new HashMap<>();
                Set<Frequency> missingWaveFrequencies = new HashSet<>(newNotes);
                    for (Long i : unfinishedSampleFragments.keySet()) {
                        Map<Frequency, Wave> oldUnfinishedSliceWaves;
                        synchronized (unfinishedSampleFragments) {
                            oldUnfinishedSliceWaves = unfinishedSampleFragments.get(i);
                        }
                        if(oldUnfinishedSliceWaves!=null) {
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
        };
    }

    private static AmplitudeState calculateAmplitudesPerFrequency(Long sampleCount, Map<Frequency, Wave> wavesPerFrequency) {
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
        return new AmplitudeState(newAmplitudeCollections);
    }
}
