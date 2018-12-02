package mixer;

import component.buffer.*;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.Envelope;
import mixer.state.EnvelopeForFrequency;
import mixer.state.VolumeState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class VolumeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesVolumeData>, BoundedBuffer<VolumeState>> buildPipe() {
        return new PipeCallable<>() {
            final Map<Long, Collection<EnvelopeForFrequency>> unfinishedSampleFragments = new ConcurrentHashMap<>();
            final Map<Long, VolumeState> finishedSampleFragments = new ConcurrentHashMap<>();

            @Override
            public BoundedBuffer<VolumeState> call(BoundedBuffer<NewNotesVolumeData> inputBuffer) {
                LinkedList<SimpleBuffer<CalculatorSampleData<Collection<EnvelopeForFrequency>, VolumeState>>> precalculatorOutputBroadcast =
                        new LinkedList<>(
                            inputBuffer
                            .performMethod(((PipeCallable<NewNotesVolumeData, Long>) this::addNewEnvelopes).toSequential(), "volume calculator - add new notes")
                            .connectTo(buildPrecalculatorPipe())
                            .broadcast(3, "volume calculator - precalculator output broadcast"));

                return
                        ((BoundedBuffer<VolumeState>) precalculatorOutputBroadcast.poll().performMethod(((PipeCallable<CalculatorSampleData<Collection<EnvelopeForFrequency>, VolumeState>, VolumeState>) CalculatorSampleData::getFinishedSampleFragmentsUntilNow).toSequential(), "volume calculator - extract finished slice until now from precalculator output"))
                        .pairWith(
                                ((BoundedBuffer<Long>) precalculatorOutputBroadcast.poll().performMethod(((PipeCallable<CalculatorSampleData<Collection<EnvelopeForFrequency>, VolumeState>, Long>) CalculatorSampleData::getSampleCount).toSequential(), "volume calculator - extract sample count from precalculator output"))
                                .pairWith(
                                        ((BoundedBuffer<Collection<EnvelopeForFrequency>>) precalculatorOutputBroadcast.poll().performMethod(((PipeCallable<CalculatorSampleData<Collection<EnvelopeForFrequency>, VolumeState>, Collection<EnvelopeForFrequency>>) CalculatorSampleData::getFinalUnfinishedSampleFragments).toSequential(), "volume calculator - extract final unfinished slice from precalculator output"))
                                        .performMethod(((PipeCallable<Collection<EnvelopeForFrequency>, Map<Frequency, Collection<Envelope>>>) VolumeCalculator::groupEnvelopesByFrequency).toSequential(), "volume calculator - group envelopes by frequency"), "volume calculator - pair sample count and envelopes grouped by frequency")
                                .performMethod(((PipeCallable<AbstractMap.SimpleImmutableEntry<Long, Map<Frequency, Collection<Envelope>>>, Map<Frequency, Collection<Double>>>) input -> calculateVolumesPerFrequency(input.getKey(), input.getValue())).toSequential(), "volume calculator - calculate volumes per frequency")
                                .performMethod(((PipeCallable<Map<Frequency, Collection<Double>>, Map<Frequency, Double>>) VolumeCalculator::sumValuesPerFrequency).toSequential(), "volume calculator - sum values per frequency")
                                .performMethod(((PipeCallable<Map<Frequency, Double>, VolumeState>) VolumeState::new).toSequential(), "volume calculator - construct new volume state"), "volume calculator - pair old and new finished slices")
                        .performMethod(
                                ((PipeCallable<AbstractMap.SimpleImmutableEntry<VolumeState, VolumeState>, VolumeState>) input1 ->
                                        input1.getKey()
                                        .add(input1.getValue()))
                        .toSequential(), "volume calculator - add old and new finished slices");
            }

            private Long addNewEnvelopes(NewNotesVolumeData newNotesVolumeData) {
                Long sampleCount = newNotesVolumeData.getSampleCount();

                Collection<EnvelopeForFrequency> newNotesWithEnvelopes = distribute(
                        newNotesVolumeData.getEnvelope(),
                        newNotesVolumeData.getNewNotes());

                for (Long i = sampleCount; i < newNotesVolumeData.getEndingSampleCount(); i++) {
                    Collection<EnvelopeForFrequency> newUnfinishedSlice = unfinishedSampleFragments.remove(i);
                    try {
                        newUnfinishedSlice.addAll(newNotesWithEnvelopes);
                    } catch (NullPointerException e) {
                        newUnfinishedSlice = new LinkedList<>(newNotesWithEnvelopes);
                    }
                    unfinishedSampleFragments.put(i, newUnfinishedSlice);
                }

                return sampleCount;
            }

            private PipeCallable<BoundedBuffer<Long>, BoundedBuffer<CalculatorSampleData<Collection<EnvelopeForFrequency>, VolumeState>>> buildPrecalculatorPipe() {
                return inputBuffer -> {
                    SimpleBuffer<CalculatorSampleData<Collection<EnvelopeForFrequency>, VolumeState>> outputBuffer = new SimpleBuffer<>(1, "amplitude calculator - precalculator output");
                    new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {
                        @Override
                        protected void tick() {
                            try {
                                Long sampleCount = input.consume();

                                Collection<EnvelopeForFrequency> finalUnfinishedSampleFragment;
                                synchronized (unfinishedSampleFragments) {
                                    if (unfinishedSampleFragments.containsKey(sampleCount)) {
                                        finalUnfinishedSampleFragment = unfinishedSampleFragments.remove(sampleCount);
                                    } else {
                                        finalUnfinishedSampleFragment = new HashSet<>();
                                    }
                                }

                                VolumeState finishedSampleFragmentUntilNow;
                                synchronized (finishedSampleFragments) {
                                    if (finishedSampleFragments.containsKey(sampleCount)) {
                                        finishedSampleFragmentUntilNow = finishedSampleFragments.remove(sampleCount);
                                    } else {
                                        finishedSampleFragmentUntilNow = new VolumeState(new HashMap<>());
                                    }
                                }

                                output.produce(
                                        new CalculatorSampleData<>(
                                                sampleCount,
                                                finalUnfinishedSampleFragment,
                                                finishedSampleFragmentUntilNow));

//                                while (input.isEmpty()) {
//                                    //Precalculate here
//                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
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

            private VolumeState removeFinishedSliceForCalculation(Long sampleCount) {
                VolumeState oldFinishedVolumeSlice = finishedSampleFragments.remove(sampleCount);
                if (oldFinishedVolumeSlice == null) {
                    oldFinishedVolumeSlice = new VolumeState(new HashMap<>());
                }
                return oldFinishedVolumeSlice;
            }

            private Collection<EnvelopeForFrequency> removeUnfinishedSliceForCalculation(Long sampleCount) {
                Collection<EnvelopeForFrequency> currentUnfinishedSlice = unfinishedSampleFragments.remove(sampleCount);
                if (currentUnfinishedSlice == null) {
                    currentUnfinishedSlice = new HashSet<>();
                }
                return currentUnfinishedSlice;
            }
        };
    }

    private static Collection<EnvelopeForFrequency> distribute(DeterministicEnvelope envelope, Collection<Frequency> frequencies) {
        Collection<EnvelopeForFrequency> newNotesWithEnvelopes = new LinkedList<>();
        for(Frequency frequency : frequencies){
            newNotesWithEnvelopes.add(new EnvelopeForFrequency(frequency, envelope));
        }
        return newNotesWithEnvelopes;
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

    private static Map<Frequency, Double> sumValuesPerFrequency(Map<Frequency, Collection<Double>> collectionMap) {
        Map<Frequency, Double> totalMap = new HashMap<>();
        for(Frequency frequency : collectionMap.keySet()) {
            Collection<Double> collection = collectionMap.get(frequency);
            Double total = collection.stream().mapToDouble(f -> f).sum();
            totalMap.put(frequency, total);
        }
        return totalMap;
    }
}
