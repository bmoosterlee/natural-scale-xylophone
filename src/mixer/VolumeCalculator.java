package mixer;

import component.buffer.*;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.Envelope;
import mixer.state.AmplitudeState;
import mixer.state.EnvelopeForFrequency;
import mixer.state.VolumeState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class VolumeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesVolumeData>, BoundedBuffer<VolumeState>> buildPipe() {
        return new PipeCallable<>() {
            final Map<Long, Collection<EnvelopeForFrequency>> unfinishedSampleFragments = new ConcurrentHashMap<>();

            @Override
            public BoundedBuffer<VolumeState> call(BoundedBuffer<NewNotesVolumeData> inputBuffer) {
                LinkedList<SimpleBuffer<PrecalculatorOutputData<Long, Collection<EnvelopeForFrequency>, VolumeState>>> precalculatorOutputBroadcast =
                        new LinkedList<>(
                            inputBuffer
                            .performMethod(((PipeCallable<NewNotesVolumeData, Long>) this::addNewEnvelopes).toSequential(), "volume calculator - add new notes")
                            .connectTo(MapPrecalculator.buildPipe(
                                    unfinishedSampleFragments,
                                    input -> sumValuesPerFrequency(calculateVolumesPerFrequency(input.getKey(), groupEnvelopesByFrequency(input.getValue()))),
                                    VolumeState::add,
                                    HashSet::new,
                                    () -> new VolumeState(new HashMap<>())))
                            .broadcast(3, "volume calculator - precalculator output broadcast"));

                return
                        precalculatorOutputBroadcast.poll().performMethod(((PipeCallable<PrecalculatorOutputData<Long, Collection<EnvelopeForFrequency>, VolumeState>, VolumeState>) PrecalculatorOutputData::getFinishedDataUntilNow).toSequential(), "volume calculator - extract finished slice until now from precalculator output")
                        .pairWith(
                                precalculatorOutputBroadcast.poll().performMethod(((PipeCallable<PrecalculatorOutputData<Long, Collection<EnvelopeForFrequency>, VolumeState>, Long>) PrecalculatorOutputData::getIndex).toSequential(), "volume calculator - extract sample count from precalculator output")
                                .pairWith(
                                        precalculatorOutputBroadcast.poll().performMethod(((PipeCallable<PrecalculatorOutputData<Long, Collection<EnvelopeForFrequency>, VolumeState>, Collection<EnvelopeForFrequency>>) PrecalculatorOutputData::getFinalUnfinishedData).toSequential(), "volume calculator - extract final unfinished slice from precalculator output")
                                        .performMethod(((PipeCallable<Collection<EnvelopeForFrequency>, Map<Frequency, Collection<Envelope>>>) VolumeCalculator::groupEnvelopesByFrequency).toSequential(), "volume calculator - group envelopes by frequency"), "volume calculator - pair sample count and envelopes grouped by frequency")
                                .performMethod(((PipeCallable<AbstractMap.SimpleImmutableEntry<Long, Map<Frequency, Collection<Envelope>>>, Map<Frequency, Collection<Double>>>) input -> calculateVolumesPerFrequency(input.getKey(), input.getValue())).toSequential(), "volume calculator - calculate volumes per frequency")
                                .performMethod(((PipeCallable<Map<Frequency, Collection<Double>>, VolumeState>) VolumeCalculator::sumValuesPerFrequency).toSequential(), "volume calculator - sum values per frequency"), "volume calculator - pair old and new finished slices")
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
                    synchronized (unfinishedSampleFragments) {
                        Collection<EnvelopeForFrequency> newUnfinishedSlice = unfinishedSampleFragments.remove(i);
                        if(newUnfinishedSlice!=null){
                            newUnfinishedSlice.addAll(newNotesWithEnvelopes);
                        } else {
                            newUnfinishedSlice = new LinkedList<>(newNotesWithEnvelopes);
                        }
                        unfinishedSampleFragments.put(i, newUnfinishedSlice);
                    }
                }

                return sampleCount;
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

    private static VolumeState sumValuesPerFrequency(Map<Frequency, Collection<Double>> collectionMap) {
        Map<Frequency, Double> totalMap = new HashMap<>();
        for(Frequency frequency : collectionMap.keySet()) {
            Collection<Double> collection = collectionMap.get(frequency);
            Double total = collection.stream().mapToDouble(f -> f).sum();
            totalMap.put(frequency, total);
        }
        return new VolumeState(totalMap);
    }
}
