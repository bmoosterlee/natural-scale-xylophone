package mixer;

import component.buffer.BoundedBuffer;
import component.buffer.PipeCallable;
import component.buffer.SimpleBuffer;
import frequency.Frequency;
import mixer.state.AmplitudeState;
import mixer.state.Wave;
import sound.SampleRate;

import java.util.*;

class AmplitudeCalculator {

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

    static PipeCallable<BoundedBuffer<NewNotesAmplitudeData>, BoundedBuffer<AmplitudeState>> buildPipe(SampleRate sampleRate) {
        return new PipeCallable<>() {
            final Map<Long, Map<Frequency, Wave>> unfinishedWaveSlices = new HashMap<>();
            final Map<Long, AmplitudeState> finishedAmplitudeSlices = new HashMap<>();

            @Override
            public BoundedBuffer<AmplitudeState> call(BoundedBuffer<NewNotesAmplitudeData> inputBuffer) {
                LinkedList<SimpleBuffer<Long>> sampleCountBroadcast = new LinkedList<>(
                        inputBuffer.performMethod(((PipeCallable<NewNotesAmplitudeData, Long>) input -> {
                            addNewWaves(input);
                            return input.getSampleCount();
                        }).toSequential(), "amplitude calculator - add new notes")
                                .broadcast(3, "amplitude calculator - sample broadcast"));
                return sampleCountBroadcast.poll()
                        .performMethod(((PipeCallable<Long, AmplitudeState>) this::removeOldFinishedSliceForCalculation).toSequential(), "amplitude calculator - remove finished slice")
                        .pairWith(
                                sampleCountBroadcast.poll()
                                        .pairWith(
                                                sampleCountBroadcast.poll()
                                                        .performMethod(((PipeCallable<Long, Map<Frequency, Wave>>) this::removeUnfinishedSliceForCalculation).toSequential(), "amplitude calculator - remove unfinished slice"), "amplitude calculator - pair sample count and unfinished slice")
                                        .performMethod(((PipeCallable<AbstractMap.SimpleImmutableEntry<Long, Map<Frequency, Wave>>, Map<Frequency, Double>>) input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue())).toSequential(), "amplitude calculator - calculate amplitudes per frequency")
                                        .performMethod(((PipeCallable<Map<Frequency, Double>, AmplitudeState>) AmplitudeState::new).toSequential(), "amplitude calculator - construct new amplitude state"), "amplitude calculator - pair new and old finished slices")
                        .performMethod(
                                ((PipeCallable<AbstractMap.SimpleImmutableEntry<AmplitudeState, AmplitudeState>, AmplitudeState>) input1 ->
                                        input1.getKey()
                                                .add(input1.getValue()))
                                        .toSequential(), "amplitude calculator - add new and old finished slices");
            }

            private void addNewWaves(NewNotesAmplitudeData newNotesAmplitudeData) {
                Map<Frequency, Wave> newNoteWaves = reuseOrCreateNewWaves(newNotesAmplitudeData.getNewNotes(), sampleRate);

                for (Long i = newNotesAmplitudeData.getSampleCount(); i < newNotesAmplitudeData.getEndingSampleCount(); i++) {
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

            private Map<Frequency, Wave> removeUnfinishedSliceForCalculation(Long sampleCount) {
                Map<Frequency, Wave> currentUnfinishedWaveSlice;
                synchronized (unfinishedWaveSlices) {
                    currentUnfinishedWaveSlice = unfinishedWaveSlices.remove(sampleCount);
                }
                if (currentUnfinishedWaveSlice == null) {
                    currentUnfinishedWaveSlice = new HashMap<>();
                }
                return currentUnfinishedWaveSlice;
            }

            private AmplitudeState removeOldFinishedSliceForCalculation(Long sampleCount) {
                AmplitudeState oldFinishedAmplitudeSlice = finishedAmplitudeSlices.remove(sampleCount);
                if (oldFinishedAmplitudeSlice == null) {
                    oldFinishedAmplitudeSlice = new AmplitudeState(new HashMap<>());
                }
                return oldFinishedAmplitudeSlice;
            }
        };
    }
}
