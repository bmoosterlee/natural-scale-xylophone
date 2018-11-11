package mixer.state;

import component.*;
import frequency.Frequency;

public class NoteTimestamper extends TickablePipeComponent {

    public NoteTimestamper(BoundedBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> newNoteBuffer, SimpleBuffer<TimestampedFrequencies> outputBuffer) {
        super(sampleCountBuffer, outputBuffer, NoteTimestamper.build(newNoteBuffer));
    }

    public static CallableWithArguments<Long, TimestampedFrequencies> build(BoundedBuffer<Frequency> newNoteBuffer) {
        return new CallableWithArguments<>() {
            final OutputPort<Long> methodInputPort;
            final InputPort<TimestampedFrequencies> methodOutputPort;

            {
                BoundedBuffer<Long> sampleCountBuffer = new SimpleBuffer<>(1, "noteTimestamper - input");
                methodInputPort = sampleCountBuffer.createOutputPort();

                BoundedBuffer<Long>[] broadcast = sampleCountBuffer.broadcast(2).toArray(new BoundedBuffer[0]);
                methodOutputPort =
                broadcast[0]
                .pairWith(
                    broadcast[1]
                    .performMethod(input1 -> new Pulse())
                    .performMethod(Flusher.flush(newNoteBuffer)))
                .performMethod(
                        input1 ->
                        new TimestampedFrequencies(
                            input1.getKey(),
                            input1.getValue()))
                .createInputPort();
            }

            @Override
            public TimestampedFrequencies call(Long input) {
                try {
                    methodInputPort.produce(input);

                    TimestampedFrequencies result = methodOutputPort.consume();

                    return result;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

}
