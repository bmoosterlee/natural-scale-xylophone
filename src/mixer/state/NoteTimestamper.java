package mixer.state;

import component.*;
import component.buffer.*;
import frequency.Frequency;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;

public class NoteTimestamper {

    public static <A extends Packet<Long>, B extends Packet<TimestampedFrequencies>, C extends Packet<Frequency>> PipeCallable<BoundedBuffer<Long, A>, BoundedBuffer<TimestampedFrequencies, B>> buildPipe(BoundedBuffer<Frequency, C> newNoteBuffer) {
        return inputBuffer -> {
            LinkedList<BoundedBuffer<Long, A>> sampleCountBroadcast =
                    new LinkedList<>(
                            inputBuffer
                                    .broadcast(2, 100, "note timestamper - broadcast"));

            return sampleCountBroadcast.poll()
                    .pairWith(
                            sampleCountBroadcast.poll()
                                    .performMethod(
                                            ((PipeCallable<Long, Pulse>) input1 -> new Pulse()).toSequential(),
                                            100,
                                            "note timestamper - to pulse")
                                    .performMethod(
                                            Flusher.flush(newNoteBuffer).toSequential(),
                                            100,
                                            "note timestamper - flush new notes"),
                            "note timestamper - pair notes and sample count")
                    .performMethod(
                            ((PipeCallable<AbstractMap.SimpleImmutableEntry<Long, List<Frequency>>, TimestampedFrequencies>)
                                    input1 -> new TimestampedFrequencies(
                                            input1.getKey(),
                                            input1.getValue()))
                                    .toSequential(),
                            100,
                            "note timestamper - build timestamped frequencies");
        };
    }

}
