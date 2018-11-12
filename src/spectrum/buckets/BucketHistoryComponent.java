package spectrum.buckets;

import component.buffer.BoundedBuffer;
import component.buffer.InputPort;
import component.buffer.OutputPort;
import component.buffer.SimpleBuffer;
import component.utilities.TickRunner;

public class BucketHistoryComponent extends TickRunner {
    private BucketHistory bucketHistory;

    private final InputPort<Buckets> inputPort;
    private final OutputPort<Buckets> outputPort;

    public BucketHistoryComponent(BoundedBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, int size){
        bucketHistory = new PrecalculatedBucketHistory(size);

        inputPort = new InputPort<>(inputBuffer);
        outputPort = new OutputPort<>(outputBuffer);

        start();
    }

    protected void tick() {
        try {
            Buckets newBuckets = inputPort.consume();

            bucketHistory = bucketHistory.addNewBuckets(newBuckets);
            Buckets timeAveragedBuckets = bucketHistory.getTimeAveragedBuckets();

            outputPort.produce(timeAveragedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
