package spectrum.buckets;

import component.buffer.BoundedBuffer;
import component.buffer.InputPort;
import component.buffer.OutputPort;
import component.buffer.SimpleBuffer;
import component.utilities.TickRunner;

public class Multiplier extends TickRunner {

    private final double multiplier;

    private final InputPort<Buckets> input;
    private final OutputPort<Buckets> output;

    public Multiplier(BoundedBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, double multiplier) {
        this.multiplier = multiplier;

        input = new InputPort<>(inputBuffer);
        output = new OutputPort<>(outputBuffer);

        start();
    }

    protected void tick() {
        try {
            Buckets buckets = input.consume();

            Buckets multipliedBuckets = buckets.multiply(multiplier);

            output.produce(multipliedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
