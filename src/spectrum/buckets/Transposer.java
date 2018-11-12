package spectrum.buckets;

import component.buffer.BoundedBuffer;
import component.buffer.InputPort;
import component.buffer.OutputPort;
import component.buffer.SimpleBuffer;
import component.utilities.Tickable;

public class Transposer extends Tickable {

    private final int transposition;

    private final InputPort<Buckets> input;
    private final OutputPort<Buckets> output;

    public Transposer(BoundedBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, int transposition) {
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
