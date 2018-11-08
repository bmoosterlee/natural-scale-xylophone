package gui.buckets;

import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;

public class Transposer implements Runnable {

    private final int transposition;

    private final InputPort<Buckets> input;
    private final OutputPort<Buckets> output;

    public Transposer(BoundedBuffer<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, int transposition) {
        this.transposition = transposition;

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

            Buckets transposedBuckets = buckets.transpose(transposition);

            output.produce(transposedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
