package mixer;

import component.Counter;
import component.Pulse;
import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.OrderStamper;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.state.*;
import sound.SampleRate;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.Callable;

public class Mixer {

    public static <A extends Packet<Pulse>, B extends Packet<Frequency>> SimpleImmutableEntry<BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>>, BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>>> buildComponent(BoundedBuffer<Pulse, A> inputBuffer, BoundedBuffer<Frequency, B> noteInputBuffer, SampleRate sampleRate){
            LinkedList<SimpleBuffer<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>>> newNoteDataBroadcast = new LinkedList<>(
                    inputBuffer
                            .performMethod(Counter.build(), "mixer - count samples")
                            .connectTo(OrderStamper.buildPipe())
                            .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                            .performMethod(EnvelopeBuilder.buildEnvelope(sampleRate), "mixer - build envelope")
                            .<NewNotesVolumeData, OrderStampedPacket<NewNotesVolumeData>>performMethod(Mixer::extractNewNotesData, "mixer - add new notes")
                    .broadcast(2, "mixer - new note data"));

            return new SimpleImmutableEntry<>(
                    newNoteDataBroadcast.poll()
                            .connectTo(VolumeCalculator.buildPipe()),
                    newNoteDataBroadcast.poll()
                            .<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>>performMethod(input ->
                                    new NewNotesAmplitudeData(
                                            input.getSampleCount(),
                                            input.getEndingSampleCount(),
                                            input.getNewNotes()), "mixer - extract amplitude data from new note data")
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
