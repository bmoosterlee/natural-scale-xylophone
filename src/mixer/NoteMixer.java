package mixer;

import component.buffer.BoundedBuffer;
import component.buffer.Packet;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.state.EnvelopeBuilder;
import mixer.state.NoteTimestamper;
import mixer.state.TimestampedNewNotesWithEnvelope;
import sound.SampleRate;
import sound.VolumeState;

import java.util.Collection;

public class NoteMixer {

    public static <B extends Packet<Frequency>> BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>> buildComponent(BoundedBuffer<Frequency, B> noteInputBuffer, SampleRate sampleRate, BoundedBuffer<Long, OrderStampedPacket<Long>> stampedSamplesBuffer) {
        BoundedBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>> newNoteData = stampedSamplesBuffer
                .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                .performMethod(EnvelopeBuilder.buildEnvelope(sampleRate), sampleRate.sampleRate / 32, "mixer - build envelope")
                .performMethod(NoteMixer::extractNewNotesData, sampleRate.sampleRate / 32, "mixer - extract new note data");

        return newNoteData
                .connectTo(VolumeCalculator.buildPipe());
    }

    private static NewNotesVolumeData extractNewNotesData(TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope) {
        Long startingSampleCount = timestampedNewNotesWithEnvelope.getSampleCount();
        Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();
        DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
        long endingSampleCount = envelope.getEndingSampleCount();

        return new NewNotesVolumeData(startingSampleCount, endingSampleCount, newNotes, envelope);
    }
}
