package mixer.state;

import component.*;
import component.buffer.*;
import component.buffer.RunningPipeComponent;
import frequency.Frequency;

public class NoteTimestamper extends RunningPipeComponent {

    public NoteTimestamper(SimpleBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> newNoteBuffer, SimpleBuffer<TimestampedFrequencies> outputBuffer) {
        super(sampleCountBuffer, outputBuffer, NoteTimestamper.build(newNoteBuffer));
    }

    public static CallableWithArguments<Long, TimestampedFrequencies> build(BoundedBuffer<Frequency> newNoteBuffer) {
        return new CallableWithArguments<>() {
            final OutputPort<Long> methodInputPort;
            final InputPort<TimestampedFrequencies> methodOutputPort;

            {
                methodInputPort = new OutputPort<>();

                BoundedBuffer<Long>[] broadcast =
                        methodInputPort
                            .getBuffer()
                            .broadcast(2, "note timestamper - broadcast").toArray(new BoundedBuffer[0]);
                methodOutputPort =
                broadcast[0]
                .pairWith(
                    broadcast[1]
                    .performMethod(input1 -> new Pulse(), "note time stamper - to pulse")
                    .performMethod(Flusher.flush(newNoteBuffer), "flush new notes"))
                .performMethod(
                        input1 ->
                        new TimestampedFrequencies(
                            input1.getKey(),
                            input1.getValue()), "build timestamped frequencies")
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
