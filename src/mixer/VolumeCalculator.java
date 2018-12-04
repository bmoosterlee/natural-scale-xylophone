package mixer;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.Orderer;
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

                SimpleImmutableEntry<BoundedBuffer<SimpleImmutableEntry<Long, Set<SimpleImmutableEntry<Frequency, Envelope>>>, Packet<SimpleImmutableEntry<Long, Set<SimpleImmutableEntry<Frequency, Envelope>>>>>, BoundedBuffer<Set<VolumeState>, Packet<Set<VolumeState>>>> precalculatorOutputs = inputBuffer
                        .connectTo(Orderer.buildPipe("volume calculator - order input"))
                        .performMethod(((PipeCallable<NewNotesVolumeData, Long>) this::addNewNotes).toSequential(), "volume calculator - add new notes")
                        .connectTo(MapPrecalculator.buildPipe(
                                unfinishedSampleFragments,
                                input2 -> calculateVolumesPerFrequency(input2.getKey(), input2.getValue())
                        ));

                return precalculatorOutputs.getValue()
                        .<VolumeState, OrderStampedPacket<VolumeState>>performMethod(input2 -> {
                            VolumeState sum1 = new VolumeState(new HashMap<>());
                            for (VolumeState finishedSampleFragment : input2) {
                                sum1 = sum1.add(finishedSampleFragment);
                            }
                            return sum1;
                        }, "volume calculator - fold precalculated sample fragments")
                        .connectTo(Orderer.buildPipe("volume calculator - order folded precalculated sample fragments"))
                        .pairWith(
                                precalculatorOutputs.getKey()
                                        .<VolumeState, OrderStampedPacket<VolumeState>>performMethod(input1 -> {
                                                    VolumeState sum = new VolumeState(new HashMap<>());
                                                    Long sampleCount = input1.getKey();
                                                    for (SimpleImmutableEntry<Frequency, Envelope> unfinishedSampleFragment : input1.getValue()) {
                                                        sum = sum.add(
                                                                calculateVolumesPerFrequency(
                                                                        sampleCount,
                                                                        unfinishedSampleFragment));
                                                    }
                                                    return sum;
                                                },
                                                "volume calculator - final fragment calculation")
                                .connectTo(Orderer.buildPipe("volume calculator - order finished fragment calculations")),
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
