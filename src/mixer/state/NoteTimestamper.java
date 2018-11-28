package mixer.state;

import component.*;
import component.buffer.*;
import frequency.Frequency;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;

public class NoteTimestamper extends MethodPipeComponent<Long, TimestampedFrequencies> {

    public NoteTimestamper(SimpleBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> newNoteBuffer, SimpleBuffer<TimestampedFrequencies> outputBuffer) {
        super(sampleCountBuffer, outputBuffer, toMethod(buildPipe(newNoteBuffer)));
    }

    public static PipeCallable<BoundedBuffer<Long>, BoundedBuffer<TimestampedFrequencies>> buildPipe(BoundedBuffer<Frequency> newNoteBuffer) {
        return new PipeCallable<>() {
            @Override
            public BoundedBuffer<TimestampedFrequencies> call(BoundedBuffer<Long> inputBuffer) {
                LinkedList<BoundedBuffer<Long>> sampleCountBroadcast =
                        new LinkedList<>(
                                inputBuffer
                                        .broadcast(2, "note timestamper - broadcast"));

                return sampleCountBroadcast.poll()
                        .pairWith(
                                sampleCountBroadcast.poll()
                                        .performMethod(((PipeCallable<Long, Pulse>) input1 -> new Pulse()).toSequential(), "note time stamper - to pulse")
                                        .performMethod(Flusher.flush(newNoteBuffer).toSequential(), "flush new notes"))
                        .performMethod(
                                ((PipeCallable<AbstractMap.SimpleImmutableEntry<Long, List<Frequency>>, TimestampedFrequencies>)
                                        input1 -> new TimestampedFrequencies(
                                                input1.getKey(),
                                                input1.getValue()))
                                        .toSequential(), "build timestamped frequencies");
            }

            @Override
            public Boolean isParallelisable() {
                return false;
            }
        };
    }

}
