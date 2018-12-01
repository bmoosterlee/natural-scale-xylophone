package mixer;

import component.buffer.BoundedBuffer;
import component.buffer.PipeCallable;
import component.buffer.SimpleBuffer;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.Envelope;
import mixer.state.EnvelopeForFrequency;
import mixer.state.VolumeState;

import java.util.*;
import java.util.stream.Collectors;

class VolumeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesVolumeData>, BoundedBuffer<VolumeState>> buildPipe() {
        return new PipeCallable<>() {
            final Map<Long, Collection<EnvelopeForFrequency>> unfinishedEnvelopeSlices = new HashMap<>();
            final Map<Long, VolumeState> finishedVolumeSlices = new HashMap<>();

            @Override
            public BoundedBuffer<VolumeState> call(BoundedBuffer<NewNotesVolumeData> inputBuffer) {
                LinkedList<SimpleBuffer<Long>> sampleCountBroadcast = new LinkedList<>(
                        inputBuffer
                        .performMethod(((PipeCallable<NewNotesVolumeData, Long>) this::addNewEnvelopes).toSequential(), "volume calculator - add new notes")
                        .broadcast(3, "volume calculator - sample broadcast"));

                return sampleCountBroadcast.poll()
                        .performMethod(((PipeCallable<Long, VolumeState>) this::removeFinishedSliceForCalculation).toSequential(), "volume calculator - remove finished slice")
                        .pairWith(
                                sampleCountBroadcast.poll()
                                        .pairWith(
                                                sampleCountBroadcast.poll()
                                                        .performMethod(((PipeCallable<Long, Collection<EnvelopeForFrequency>>) this::removeUnfinishedSliceForCalculation).toSequential(), "volume calculator - remove unfinished slice")
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
                    Collection<EnvelopeForFrequency> newUnfinishedSlice = unfinishedEnvelopeSlices.remove(i);
                    try {
                        newUnfinishedSlice.addAll(newNotesWithEnvelopes);
                    } catch (NullPointerException e) {
                        newUnfinishedSlice = new LinkedList<>(newNotesWithEnvelopes);
                    }
                    unfinishedEnvelopeSlices.put(i, newUnfinishedSlice);
                }

                return sampleCount;
            }

            private VolumeState removeFinishedSliceForCalculation(Long sampleCount) {
                VolumeState oldFinishedVolumeSlice = finishedVolumeSlices.remove(sampleCount);
                if (oldFinishedVolumeSlice == null) {
                    oldFinishedVolumeSlice = new VolumeState(new HashMap<>());
                }
                return oldFinishedVolumeSlice;
            }

            private Collection<EnvelopeForFrequency> removeUnfinishedSliceForCalculation(Long sampleCount) {
                Collection<EnvelopeForFrequency> currentUnfinishedSlice = unfinishedEnvelopeSlices.remove(sampleCount);
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
