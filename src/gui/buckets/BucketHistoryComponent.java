package gui.buckets;

import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;

public class BucketHistoryComponent implements Runnable{
    private BucketHistory bucketHistory;

    private final InputPort<Buckets> newBucketsInput;
    private final OutputPort<Buckets> timeAveragedBucketsOutput;

    public BucketHistoryComponent(int size, BoundedBuffer<Buckets> newBucketsBuffer, BoundedBuffer<Buckets> timeAveragedBucketsBuffer){
        bucketHistory = new PrecalculatedBucketHistory(size);

        newBucketsInput = new InputPort<>(newBucketsBuffer);
        timeAveragedBucketsOutput = new OutputPort<>(timeAveragedBucketsBuffer);

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
            Buckets newBuckets = newBucketsInput.consume();

            bucketHistory = bucketHistory.addNewBuckets(newBuckets);
            Buckets timeAveragedBuckets = bucketHistory.getTimeAveragedBuckets();

            timeAveragedBucketsOutput.produce(timeAveragedBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
