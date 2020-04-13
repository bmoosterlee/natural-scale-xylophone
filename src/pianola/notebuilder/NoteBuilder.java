package notebuilder;

import component.buffer.BoundedBuffer;
import component.buffer.Packet;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import notebuilder.envelope.DeterministicEnvelope;
import notebuilder.state.EnvelopeBuilder;
import notebuilder.state.NoteTimestamper;
import notebuilder.state.TimestampedNewNotesWithEnvelope;
import sound.SampleRate;
import sound.VolumeState;

import java.util.Collection;

public class NoteBuilder {

    public static <B extends Packet<Frequency>> BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>> buildComponent(BoundedBuffer<Frequency, B> noteInputBuffer, SampleRate sampleRate, BoundedBuffer<Long, OrderStampedPacket<Long>> stampedSamplesBuffer) {
        BoundedBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>> newNoteData = stampedSamplesBuffer
                .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                .performMethod(EnvelopeBuilder.buildEnvelope(sampleRate), sampleRate.sampleRate / 32, "notebuilder - build envelope")
                .performMethod(NoteBuilder::extractNewNotesData, sampleRate.sampleRate / 32, "notebuilder - extract new note data");

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
