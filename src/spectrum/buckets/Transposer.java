package spectrum.buckets;

import component.*;

public class Transposer extends Tickable {

    private final int transposition;

    private final InputPort<Buckets> input;
    private final OutputPort<Buckets> output;

    public Transposer(BufferInterface<Buckets> inputBuffer, BoundedBuffer<Buckets> outputBuffer, int transposition) {
        this.transposition = transposition;

        input = new InputPort<>(inputBuffer);
        output = new OutputPort<>(outputBuffer);

        start();
    }

    protected void tick() {
        try {
            Buckets buckets = input.consume();
            Buckets transposedBuckets = buckets.transpose(transposition);
            output.produce(transposedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
