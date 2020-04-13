package mixer;

import component.buffer.BoundedBuffer;
import component.buffer.Packet;
import component.buffer.SimpleBuffer;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.state.*;
import sound.SampleRate;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.LinkedList;

public class Mixer {

    public static <B extends Packet<Frequency>> SimpleImmutableEntry<BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>>, BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>>> buildComponent(BoundedBuffer<Frequency, B> noteInputBuffer, SampleRate sampleRate, BoundedBuffer<Long, OrderStampedPacket<Long>> stampedSamplesBuffer) {
        LinkedList<SimpleBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>>> newNoteDataBroadcast = new LinkedList<>(
                    stampedSamplesBuffer
                            .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                            .performMethod(EnvelopeBuilder.buildEnvelope(sampleRate), sampleRate.sampleRate / 32, "mixer - build envelope")
                            .<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>>performMethod(Mixer::extractNewNotesData, sampleRate.sampleRate / 32, "mixer - extract new note data")
                    .broadcast(2, 100, "mixer - new note data"));

        return new SimpleImmutableEntry<>(
                newNoteDataBroadcast.poll()
                        .connectTo(VolumeCalculator.buildPipe()),
                newNoteDataBroadcast.poll()
                        .<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>>performMethod(input ->
                                new NewNotesAmplitudeData(
                                        input.getSampleCount(),
                                        input.getEndingSampleCount(),
                                        input.getNewNotes()), sampleRate.sampleRate / 32, "mixer - extract amplitude data from new note data")
                        .connectTo(AmplitudeCalculator.buildPipe(sampleRate)));
    }

    private static NewNotesVolumeData extractNewNotesData(TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope) {
        Long startingSampleCount = timestampedNewNotesWithEnvelope.getSampleCount();
        Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();
        DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
        long endingSampleCount = envelope.getEndingSampleCount();

        return new NewNotesVolumeData(startingSampleCount, endingSampleCount, newNotes, envelope);
    }
}
