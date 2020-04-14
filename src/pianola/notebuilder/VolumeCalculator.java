package pianola.notebuilder;

import component.buffer.BoundedBuffer;
import component.buffer.PipeCallable;
import component.buffer.SimplePacket;
import frequency.Frequency;
import pianola.notebuilder.envelope.DeterministicEnvelope;
import pianola.notebuilder.envelope.Envelope;
import sound.VolumeStateMap;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class VolumeCalculator extends SampleCalculator{

    static PipeCallable<BoundedBuffer<NewNotesVolumeData, SimplePacket<NewNotesVolumeData>>, BoundedBuffer<VolumeStateMap, SimplePacket<VolumeStateMap>>> buildPipe() {
        return new PipeCallable<>() {
            final ConcurrentHashMap<Long, Set<SimpleImmutableEntry<Frequency, Envelope>>> unfinishedSampleFragments = new ConcurrentHashMap<>();

            @Override
            public BoundedBuffer<VolumeStateMap, SimplePacket<VolumeStateMap>> call(BoundedBuffer<NewNotesVolumeData, SimplePacket<NewNotesVolumeData>> inputBuffer) {
                return buildSampleCalculator(
                        inputBuffer,
                        unfinishedSampleFragments,
                        this::addNewNotes,
                        input -> calculateVolumesPerFrequency(input.getKey(), input.getValue()),
                        VolumeStateMap::add,
                        () -> new VolumeStateMap(new HashMap<>()),
                        "volume calculator");
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
                            unfinishedSampleFragments.put(i, Collections.synchronizedSet(new HashSet<>(newNotesWithEnvelopes)));
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

    private static VolumeStateMap calculateVolumesPerFrequency(Long sampleCount, SimpleImmutableEntry<Frequency, Envelope> envelopePerFrequency) {
        return new VolumeStateMap(Collections.singletonMap(envelopePerFrequency.getKey(), envelopePerFrequency.getValue().getVolume(sampleCount)));
    }
}
