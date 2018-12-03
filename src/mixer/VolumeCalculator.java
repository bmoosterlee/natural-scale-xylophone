package mixer;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.state.EnvelopeForFrequency;
import mixer.state.VolumeState;

import java.util.*;

class VolumeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>>, BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>>> buildPipe() {
        return new PipeCallable<>() {
            final Map<Long, Set<EnvelopeForFrequency>> unfinishedSampleFragments = new HashMap<>();

            @Override
            public BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>> call(BoundedBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>> inputBuffer) {
                return inputBuffer
                        .performMethod(((PipeCallable<NewNotesVolumeData, Long>) this::addNewNotes).toSequential(), "volume calculator - add new notes")
                        .connectTo(MapPrecalculator.buildPipe(
                                unfinishedSampleFragments,
                                input2 -> calculateVolumesPerFrequency(input2.getKey(), input2.getValue())
                        ))
                        .performMethod(input -> {
                                VolumeState sum = new VolumeState(new HashMap<>());
                                for(VolumeState finishedSampleFragment : input.getFinishedDataUntilNow()){
                                    sum = sum.add(finishedSampleFragment);
                                }
                                for(EnvelopeForFrequency unfinishedSampleFragment : input.getFinalUnfinishedData()){
                                    sum = sum.add(
                                                calculateVolumesPerFrequency(
                                                    input.getIndex(),
                                                    unfinishedSampleFragment));
                                }
                                return sum;
                        });
            }

            private Long addNewNotes(NewNotesVolumeData newNotesVolumeData) {
                Long sampleCount = newNotesVolumeData.getSampleCount();

                Collection<Frequency> newNotes = newNotesVolumeData.getNewNotes();

                if(!newNotes.isEmpty()) {
                    Collection<EnvelopeForFrequency> newNotesWithEnvelopes = distribute(
                            newNotesVolumeData.getEnvelope(),
                            newNotes);

                    for (Long i = sampleCount; i <= newNotesVolumeData.getEndingSampleCount(); i++) {
                        Set<EnvelopeForFrequency> unfinishedFragmentsForThisSample = unfinishedSampleFragments.get(i);
                        if (unfinishedFragmentsForThisSample != null) {
                            synchronized (unfinishedFragmentsForThisSample) {
                                unfinishedFragmentsForThisSample.addAll(newNotesWithEnvelopes);
                            }
                        } else {
                            synchronized (unfinishedSampleFragments) {
                                unfinishedSampleFragments.put(i, new HashSet<>(newNotesWithEnvelopes));
                            }
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

    private static VolumeState calculateVolumesPerFrequency(Long sampleCount, EnvelopeForFrequency envelopePerFrequency) {
        return new VolumeState(Collections.singletonMap(envelopePerFrequency.getFrequency(), envelopePerFrequency.getEnvelope().getVolume(sampleCount)));
    }
}
