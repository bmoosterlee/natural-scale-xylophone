package mixer.state;

import frequency.Frequency;
import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;

import java.util.List;

public class NoteTimestamper implements Runnable {

    private final InputPort<Long> sampleCountInput;
    private final InputPort<Frequency> newNoteInput;
    private final OutputPort<TimestampedFrequencies> output;

    public NoteTimestamper(BoundedBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> newNoteBuffer, BoundedBuffer<TimestampedFrequencies> outputBuffer) {
        sampleCountInput = new InputPort<>(sampleCountBuffer);
        newNoteInput = new InputPort<>(newNoteBuffer);
        output = new OutputPort<>(outputBuffer);

        start();
    }

    private void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    private void tick() {
        try {
            Long sampleCount = sampleCountInput.consume();
            List<Frequency> newNotes = newNoteInput.flush();

            TimestampedFrequencies timestampedFrequencies = new TimestampedFrequencies(sampleCount, newNotes);

            output.produce(timestampedFrequencies);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
