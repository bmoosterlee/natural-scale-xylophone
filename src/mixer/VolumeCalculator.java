package mixer;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.Envelope;
import mixer.state.VolumeState;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.ConcurrentHashMap;

class VolumeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>>, BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>>> buildPipe() {
        return new PipeCallable<>() {
            final ConcurrentHashMap<Long, Set<SimpleImmutableEntry<Frequency, Envelope>>> unfinishedSampleFragments = new ConcurrentHashMap<>();

            @Override
            public BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>> call(BoundedBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>> inputBuffer) {

                LinkedList<SimpleBuffer<PrecalculatorOutputData<Long, Set<SimpleImmutableEntry<Frequency, Envelope>>, Set<VolumeState>>, Packet<PrecalculatorOutputData<Long, Set<SimpleImmutableEntry<Frequency, Envelope>>, Set<VolumeState>>>>> precalculatorBroadcast = new LinkedList<>(inputBuffer
                        .performMethod(((PipeCallable<NewNotesVolumeData, Long>) this::addNewNotes).toSequential(), "volume calculator - add new notes")
                        .connectTo(MapPrecalculator.buildPipe(
                                unfinishedSampleFragments,
                                input2 -> calculateVolumesPerFrequency(input2.getKey(), input2.getValue())
                        )).broadcast(2, "volume calculator - precalculator broadcast"));

                return precalculatorBroadcast.poll()
                        .performMethod(input -> {
                                VolumeState sum = new VolumeState(new HashMap<>());
                                for(VolumeState finishedSampleFragment : input.getFinishedDataUntilNow()){
                                    sum = sum.add(finishedSampleFragment);
                                }
                                return sum;
                        }, "volume calculator - fold precalculated sample fragments")
                        .pairWith(
                                precalculatorBroadcast.poll()
                                        .performMethod(input -> {
                                            VolumeState sum = new VolumeState(new HashMap<>());
                                            for(SimpleImmutableEntry<Frequency, Envelope> unfinishedSampleFragment : input.getFinalUnfinishedData()){
                                                sum = sum.add(
                                                        calculateVolumesPerFrequency(
                                                                input.getIndex(),
                                                                unfinishedSampleFragment));
                                            }
                                            return sum;
                                        },
                                        "volume calculator - final fragment calculation"),
                                "volume calculator - pair calculated and precalculated sample fragments")
                        .performMethod(input -> input.getKey().add(input.getValue()), "volume calculator - construct finished sample");
            }

            private Long addNewNotes(NewNotesVolumeData newNotesVolumeData) {
                Long sampleCount = newNotesVolumeData.getSampleCount();

                Collection<Frequency> newNotes = newNotesVolumeData.getNewNotes();

                if(!newNotes.isEmpty()) {
                    Collection<SimpleImmutableEntry<Frequency, Envelope>> newNotesWithEnvelopes = distribute(
                            newNotesVolumeData.getEnvelope(),
                            newNotes);

                    for (Long i = sampleCount; i <= newNotesVolumeData.getEndingSampleCount(); i++) {
                        Set<SimpleImmutableEntry<Frequency, Envelope>> unfinishedFragmentsForThisSample = unfinishedSampleFragments.get(i);
                        if (unfinishedFragmentsForThisSample != null) {
                            unfinishedFragmentsForThisSample.addAll(newNotesWithEnvelopes);
                        } else {
                            unfinishedSampleFragments.put(i, new HashSet<>(newNotesWithEnvelopes));
                        }
                    }
                }
                return sampleCount;
            }
        };
    }

    private static Collection<SimpleImmutableEntry<Frequency, Envelope>> distribute(DeterministicEnvelope envelope, Collection<Frequency> frequencies) {
        Collection<SimpleImmutableEntry<Frequency, Envelope>> newNotesWithEnvelopes = new LinkedList<>();
        for(Frequency frequency : frequencies){
            newNotesWithEnvelopes.add(new SimpleImmutableEntry<>(frequency, envelope));
        }
        return newNotesWithEnvelopes;
    }

    private static VolumeState calculateVolumesPerFrequency(Long sampleCount, SimpleImmutableEntry<Frequency, Envelope> envelopePerFrequency) {
        return new VolumeState(Collections.singletonMap(envelopePerFrequency.getKey(), envelopePerFrequency.getValue().getVolume(sampleCount)));
    }
}
