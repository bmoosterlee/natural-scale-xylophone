package mixer;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.OrderStamper;
import component.orderer.Orderer;
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

    static PipeCallable<BoundedBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>>, BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>>> buildPipe() {
        return new PipeCallable<>() {
            final Map<Long, Collection<EnvelopeForFrequency>> unfinishedSampleFragments = new ConcurrentHashMap<>();

            @Override
            public BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>> call(BoundedBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>> inputBuffer) {
                LinkedList<SimpleBuffer<PrecalculatorOutputData<Long, Collection<EnvelopeForFrequency>, VolumeState>, OrderStampedPacket<PrecalculatorOutputData<Long, Collection<EnvelopeForFrequency>, VolumeState>>>> precalculatorOutputBroadcast =
                        new LinkedList<>(
                            inputBuffer
                                    .performMethod(this::addNewEnvelopes, "volume calculator - add new notes")
                                    .<PrecalculatorOutputData<Long, Collection<EnvelopeForFrequency>, VolumeState>, OrderStampedPacket<PrecalculatorOutputData<Long, Collection<EnvelopeForFrequency>, VolumeState>>>connectTo(MapPrecalculator.buildPipe(
                                            unfinishedSampleFragments,
                                            input2 -> sumValuesPerFrequency(calculateVolumesPerFrequency(input2.getKey(), groupEnvelopesByFrequency(input2.getValue()))),
                                            VolumeState::add,
                                            HashSet::new,
                                            () -> new VolumeState(new HashMap<>())))
                            .broadcast(3, "volume calculator - precalculator output broadcast"));

                return
                        precalculatorOutputBroadcast.poll().<VolumeState, OrderStampedPacket<VolumeState>>performMethod(PrecalculatorOutputData::getFinishedDataUntilNow, "volume calculator - extract finished slice until now from precalculator output")
                        .connectTo(Orderer.buildPipe())
                        .pairWith(
                                precalculatorOutputBroadcast.poll().<Long, OrderStampedPacket<Long>>performMethod(PrecalculatorOutputData::getIndex, "volume calculator - extract sample count from precalculator output")
                                        .connectTo(Orderer.buildPipe())
                                        .pairWith(
                                                precalculatorOutputBroadcast.poll().performMethod(PrecalculatorOutputData::getFinalUnfinishedData, "volume calculator - extract final unfinished slice from precalculator output")
                                                        .<Map<Frequency, Collection<Envelope>>, OrderStampedPacket<Map<Frequency, Collection<Envelope>>>>performMethod(VolumeCalculator::groupEnvelopesByFrequency, "volume calculator - group envelopes by frequency")
                                                        .connectTo(Orderer.buildPipe()),
                                                "volume calculator - pair sample count and envelopes grouped by frequency")
                                        .performMethod(input -> calculateVolumesPerFrequency(input.getKey(), input.getValue()), "volume calculator - calculate volumes per frequency")
                                        .<VolumeState, OrderStampedPacket<VolumeState>>performMethod(VolumeCalculator::sumValuesPerFrequency, "volume calculator - sum values per frequency")
                                .connectTo(Orderer.buildPipe()),
                                "volume calculator - pair old and new finished slices")
                        .performMethod(
                                input1 ->
                                        input1.getKey()
                                        .add(input1.getValue())
                        , "volume calculator - add old and new finished slices");
            }

            private Long addNewEnvelopes(NewNotesVolumeData newNotesVolumeData) {
                Long sampleCount = newNotesVolumeData.getSampleCount();

                Collection<Frequency> newNotes = newNotesVolumeData.getNewNotes();

                if(!newNotes.isEmpty()) {
                    Collection<EnvelopeForFrequency> newNotesWithEnvelopes = distribute(
                            newNotesVolumeData.getEnvelope(),
                            newNotes);

                    for (Long i = sampleCount; i < newNotesVolumeData.getEndingSampleCount(); i++) {
                        synchronized (unfinishedSampleFragments) {
                            Collection<EnvelopeForFrequency> newUnfinishedSlice = unfinishedSampleFragments.remove(i);
                            if (newUnfinishedSlice != null) {
                                newUnfinishedSlice.addAll(newNotesWithEnvelopes);
                            } else {
                                newUnfinishedSlice = new LinkedList<>(newNotesWithEnvelopes);
                            }
                            unfinishedSampleFragments.put(i, newUnfinishedSlice);
                        }
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