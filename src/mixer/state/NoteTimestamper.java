package mixer.state;

import component.*;
import component.buffer.*;
import component.buffer.RunningPipeComponent;
import frequency.Frequency;

import java.util.LinkedList;

public class NoteTimestamper extends RunningPipeComponent {

    public NoteTimestamper(SimpleBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> newNoteBuffer, SimpleBuffer<TimestampedFrequencies> outputBuffer) {
        super(sampleCountBuffer, outputBuffer, NoteTimestamper.build(newNoteBuffer));
    }

    public static CallableWithArguments<Long, TimestampedFrequencies> build(BoundedBuffer<Frequency> newNoteBuffer) {
        return new CallableWithArguments<>() {
            final OutputPort<Long> sampleCountPort;
            final InputPort<TimestampedFrequencies> methodOutputPort;

            {
                sampleCountPort = new OutputPort<>();

                LinkedList<BoundedBuffer<Long>> sampleCountBroadcast =
                        new LinkedList<>(
                            sampleCountPort
                                .getBuffer()
                                .broadcast(2, "note timestamper - broadcast"));

                methodOutputPort =
                sampleCountBroadcast.poll()
                .pairWith(
                    sampleCountBroadcast.poll()
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
                    sampleCountPort.produce(input);

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
