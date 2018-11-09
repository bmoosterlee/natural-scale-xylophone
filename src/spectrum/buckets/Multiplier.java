package spectrum.buckets;

import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;
import component.Tickable;

public class Multiplier extends Tickable {

    private final double multiplier;

    private final InputPort<Buckets> input;
    private final OutputPort<Buckets> output;

    public Multiplier(BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, double multiplier) {
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
