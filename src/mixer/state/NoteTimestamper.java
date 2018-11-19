package mixer.state;

import component.*;
import component.buffer.*;
import frequency.Frequency;

import java.util.LinkedList;

public class NoteTimestamper extends MethodPipeComponent<Long, TimestampedFrequencies> {

    public NoteTimestamper(SimpleBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> newNoteBuffer, SimpleBuffer<TimestampedFrequencies> outputBuffer) {
        super(sampleCountBuffer, outputBuffer, toMethod(buildPipe(newNoteBuffer)));
    }

    public static CallableWithArguments<BoundedBuffer<Long>, BoundedBuffer<TimestampedFrequencies>> buildPipe(BoundedBuffer<Frequency> newNoteBuffer) {
        return inputBuffer -> {
            LinkedList<BoundedBuffer<Long>> sampleCountBroadcast =
                    new LinkedList<>(
                        inputBuffer
                            .broadcast(2, "note timestamper - broadcast"));

            return sampleCountBroadcast.poll()
                .pairWith(
                    sampleCountBroadcast.poll()
                    .performMethod(input1 -> new Pulse(), "note time stamper - to pulse")
                    .performMethod(Flusher.flush(newNoteBuffer), "flush new notes"))
                .performMethod(
                    input1 ->
                        new TimestampedFrequencies(
                            input1.getKey(),
                            input1.getValue()), "build timestamped frequencies");
        };
    }

}
