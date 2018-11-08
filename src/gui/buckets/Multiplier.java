package gui.buckets;

import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;

public class Multiplier implements Runnable {

    private final double multiplier;

    private final InputPort<Buckets> input;
    private final OutputPort<Buckets> output;

    public Multiplier(BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, double multiplier) {
        this.multiplier = multiplier;

        input = new InputPort<>(inputBuffer);
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
            Buckets buckets = input.consume();

            Buckets multipliedBuckets = buckets.multiply(multiplier);

            output.produce(multipliedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
