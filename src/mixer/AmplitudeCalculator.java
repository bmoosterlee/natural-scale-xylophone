package mixer;

import component.buffer.*;
import frequency.Frequency;
import mixer.state.AmplitudeState;
import mixer.state.Wave;
import sound.SampleRate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class AmplitudeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesAmplitudeData>, BoundedBuffer<AmplitudeState>> buildPipe(SampleRate sampleRate) {
        return new PipeCallable<>() {
            final Map<Long, Map<Frequency, Wave>> unfinishedSampleFragments = new ConcurrentHashMap<>();
            final Map<Long, AmplitudeState> finishedSampleFragments = new ConcurrentHashMap<>();

            @Override
            public BoundedBuffer<AmplitudeState> call(BoundedBuffer<NewNotesAmplitudeData> inputBuffer) {
                LinkedList<SimpleBuffer<CalculatorSampleData<Map<Frequency, Wave>, AmplitudeState>>> precalculatorOutputBroadcast =
                        new LinkedList<>(
                                inputBuffer
                                .performMethod(((PipeCallable<NewNotesAmplitudeData, Long>) this::addNewWaves).toSequential(), "amplitude calculator - add new notes")
                                .connectTo(buildPrecalculatorPipe())
                                .broadcast(3, "precalculator output - broadcast"));

                return
                    precalculatorOutputBroadcast.poll()
                    .performMethod(((PipeCallable<CalculatorSampleData<Map<Frequency, Wave>, AmplitudeState>, AmplitudeState>) CalculatorSampleData::getFinishedSampleFragmentsUntilNow).toSequential(), "amplitude calculator - remove finished slice")
                    .pairWith(
                            precalculatorOutputBroadcast.poll()
                            .performMethod(((PipeCallable<CalculatorSampleData<Map<Frequency, Wave>, AmplitudeState>, Long>) CalculatorSampleData::getSampleCount).toSequential(), "amplitude calculator - extract sample count from precalculator")
                            .pairWith(
                                    precalculatorOutputBroadcast.poll()
                                    .performMethod(((PipeCallable<CalculatorSampleData<Map<Frequency, Wave>, AmplitudeState>, Map<Frequency, Wave>>) CalculatorSampleData::getFinalUnfinishedSampleFragments).toSequential(), "amplitude calculator - remove unfinished slice"), "amplitude calculator - pair sample count and unfinished slice")
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
                    synchronized (unfinishedSampleFragments) {
                        Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSampleFragments.get(i);
                        try {
                            Map<Frequency, Wave> missingNoteWaves = new HashMap<>(newNoteWaves);
                            missingNoteWaves.keySet().removeAll(oldUnfinishedSliceWaves.keySet());
                            oldUnfinishedSliceWaves.putAll(missingNoteWaves);
                        } catch (NullPointerException e) {
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
                synchronized (unfinishedSampleFragments) {
                    for (Long i : unfinishedSampleFragments.keySet()) {
                        Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSampleFragments.get(i);

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

            private PipeCallable<BoundedBuffer<Long>, BoundedBuffer<CalculatorSampleData<Map<Frequency, Wave>, AmplitudeState>>> buildPrecalculatorPipe() {
                return inputBuffer -> {
                    SimpleBuffer<CalculatorSampleData<Map<Frequency, Wave>, AmplitudeState>> outputBuffer = new SimpleBuffer<>(1, "amplitude calculator - precalculator output");
                    new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {
                        @Override
                        protected void tick() {
                            try {
                                Long sampleCount = input.consume();

                                Map<Frequency, Wave> finalUnfinishedSampleFragment;
                                synchronized (unfinishedSampleFragments) {
                                    if (unfinishedSampleFragments.containsKey(sampleCount)) {
                                        finalUnfinishedSampleFragment = unfinishedSampleFragments.remove(sampleCount);
                                    } else {
                                        finalUnfinishedSampleFragment = new HashMap<>();
                                    }
                                }
                                AmplitudeState finishedSampleFragmentUntilNow;
                                synchronized (finishedSampleFragments) {
                                    if (finishedSampleFragments.containsKey(sampleCount)) {
                                        finishedSampleFragmentUntilNow = finishedSampleFragments.remove(sampleCount);
                                    } else {
                                        finishedSampleFragmentUntilNow = new AmplitudeState(new HashMap<>());
                                    }
                                }

                                output.produce(
                                        new CalculatorSampleData<>(
                                                sampleCount,
                                                finalUnfinishedSampleFragment,
                                                finishedSampleFragmentUntilNow));

                                calculateAmplitudesContinuously();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        private void calculateAmplitudesContinuously() {
                            Iterator<Long> unfinishedSampleFragmentsIterator = unfinishedSampleFragments.keySet().iterator();
                            while (input.isEmpty() && unfinishedSampleFragmentsIterator.hasNext()) {
                                if(unfinishedSampleFragmentsIterator.hasNext()) {
                                    Long unfinshedSampleCount = unfinishedSampleFragmentsIterator.next();
                                    Map<Frequency, Wave> unfinishedSampleFragment;
                                    synchronized (unfinishedSampleFragments) {
                                        unfinishedSampleFragment = unfinishedSampleFragments.remove(unfinshedSampleCount);
                                    }
                                    finishedSampleFragments.put(
                                            unfinshedSampleCount,
                                            new AmplitudeState(
                                                calculateAmplitudesPerFrequency(
                                                        unfinshedSampleCount,
                                                        unfinishedSampleFragment)));
                                }
                            }
                        }

                        @Override
                        public Boolean isParallelisable() {
                            return false;
                        }
                    });
                    return outputBuffer;
                };
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
