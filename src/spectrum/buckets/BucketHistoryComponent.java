package spectrum.buckets;

import component.buffers.BoundedBuffer;
import component.buffers.InputPort;
import component.buffers.OutputPort;
import component.buffers.SimpleBuffer;
import component.utilities.Tickable;

public class BucketHistoryComponent extends Tickable {
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
