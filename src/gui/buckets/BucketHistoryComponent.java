package gui.buckets;

import component.BoundedBuffer;
import component.Component;
import component.InputPort;
import component.OutputPort;

public class BucketHistoryComponent extends Component {
    private BucketHistory bucketHistory;

    private final InputPort<Buckets> newBucketsInput;
    private final OutputPort<Buckets> timeAveragedBucketsOutput;

    public BucketHistoryComponent(int size, BoundedBuffer<Buckets> newBucketsBuffer, BoundedBuffer<Buckets> timeAveragedBucketsBuffer){
        bucketHistory = new PrecalculatedBucketHistory(size);

        newBucketsInput = new InputPort<>(newBucketsBuffer);
        timeAveragedBucketsOutput = new OutputPort<>(timeAveragedBucketsBuffer);

        start();
    }

    protected void tick() {
        try {
            Buckets newBuckets = newBucketsInput.consume();

            bucketHistory = bucketHistory.addNewBuckets(newBuckets);
            Buckets timeAveragedBuckets = bucketHistory.getTimeAveragedBuckets();

            timeAveragedBucketsOutput.produce(timeAveragedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
