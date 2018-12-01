package mixer;

import component.buffer.*;
import frequency.Frequency;
import mixer.state.AmplitudeState;
import mixer.state.Wave;
import sound.SampleRate;

import java.nio.channels.Pipe;
import java.util.*;

class AmplitudeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesAmplitudeData>, BoundedBuffer<AmplitudeState>> buildPipe(SampleRate sampleRate) {
        return new PipeCallable<>() {
            final Map<Long, Map<Frequency, Wave>> unfinishedWaveSlices = new HashMap<>();
            final Map<Long, AmplitudeState> finishedAmplitudeSlices = new HashMap<>();

            @Override
            public BoundedBuffer<AmplitudeState> call(BoundedBuffer<NewNotesAmplitudeData> inputBuffer) {
                LinkedList<SimpleBuffer<CalculatorSlice<Map<Frequency, Wave>, AmplitudeState>>> precalculatorOutputBroadcast =
                        new LinkedList<>(
                                inputBuffer
                                .performMethod(((PipeCallable<NewNotesAmplitudeData, Long>) this::addNewWaves).toSequential(), "amplitude calculator - add new notes")
                                .connectTo(buildPrecalculatorPipe())
                                .broadcast(3, "precalculator output - broadcast"));

                return
                    precalculatorOutputBroadcast.poll()
                    .performMethod(((PipeCallable<CalculatorSlice<Map<Frequency, Wave>, AmplitudeState>, AmplitudeState>) CalculatorSlice::getFinishedSliceUntilNow).toSequential(), "amplitude calculator - remove finished slice")
                    .pairWith(
                            precalculatorOutputBroadcast.poll()
                            .performMethod(((PipeCallable<CalculatorSlice<Map<Frequency, Wave>, AmplitudeState>, Long>) CalculatorSlice::getSampleCount).toSequential(), "amplitude calculator - extract sample count from precalculator")
                            .pairWith(
                                    precalculatorOutputBroadcast.poll()
                                    .performMethod(((PipeCallable<CalculatorSlice<Map<Frequency, Wave>, AmplitudeState>, Map<Frequency, Wave>>) CalculatorSlice::getFinalUnfinishedSlice).toSequential(), "amplitude calculator - remove unfinished slice"), "amplitude calculator - pair sample count and unfinished slice")
                            .performMethod(((PipeCallable<AbstractMap.SimpleImmutableEntry<Long, Map<Frequency, Wave>>, Map<Frequency, Double>>) input -> calculateAmplitudesPerFrequency(input.getKey(), input.getValue())).toSequential(), "amplitude calculator - calculate amplitudes per frequency")
                            .performMethod(((PipeCallable<Map<Frequency, Double>, AmplitudeState>) AmplitudeState::new).toSequential(), "amplitude calculator - construct new amplitude state"), "amplitude calculator - pair new and old finished slices")
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
                return sampleCount;
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

            private PipeCallable<BoundedBuffer<Long>, BoundedBuffer<CalculatorSlice<Map<Frequency, Wave>, AmplitudeState>>> buildPrecalculatorPipe() {
                return inputBuffer -> {
                    SimpleBuffer<CalculatorSlice<Map<Frequency, Wave>, AmplitudeState>> outputBuffer = new SimpleBuffer<>(1, "amplitude calculator - precalculator output");
                    new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {
                        @Override
                        protected void tick() {
                            try {
                                Long sampleCount = input.consume();

                                Map<Frequency, Wave> unfinishedSlice;
                                synchronized (unfinishedWaveSlices) {
                                    if (unfinishedWaveSlices.containsKey(sampleCount)) {
                                        unfinishedSlice = unfinishedWaveSlices.remove(sampleCount);
                                    } else {
                                        unfinishedSlice = new HashMap<>();
                                    }
                                }

                                AmplitudeState finishedSlice;
                                synchronized (finishedAmplitudeSlices) {
                                    if (finishedAmplitudeSlices.containsKey(sampleCount)) {
                                        finishedSlice = finishedAmplitudeSlices.remove(sampleCount);
                                    } else {
                                        finishedSlice = new AmplitudeState(new HashMap<>());
                                    }
                                }

                                output.produce(
                                        new CalculatorSlice<>(
                                                sampleCount,
                                                unfinishedSlice,
                                                finishedSlice));

//                                while (input.isEmpty()) {
//                                    //Precalculate here
//                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    return outputBuffer;
                };
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
}
