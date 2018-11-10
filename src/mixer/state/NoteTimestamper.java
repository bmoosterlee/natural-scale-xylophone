package mixer.state;

import component.*;
import frequency.Frequency;

import java.util.AbstractMap;
import java.util.Collection;

public class NoteTimestamper implements Component {

    public NoteTimestamper(BoundedBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> newNoteBuffer, BoundedBuffer<TimestampedFrequencies> outputBuffer) {
        BoundedBuffer<Long>[] broadcast = sampleCountBuffer.broadcast(2).toArray(new BoundedBuffer[0]);
        broadcast[0]
                .pairWith(Flusher.flush(broadcast[1]
                        .performMethod(input -> new Pulse()), newNoteBuffer))
                .performMethod(input -> new TimestampedFrequencies(input.getKey(), input.getValue()))
                .relayTo(outputBuffer);
    }

}
