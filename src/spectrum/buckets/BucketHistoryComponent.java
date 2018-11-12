package spectrum.buckets;

import component.buffer.*;
import component.utilities.RunningPipeComponent;
import component.utilities.TickRunner;

public class BucketHistoryComponent extends RunningPipeComponent<Buckets, Buckets> {

    public BucketHistoryComponent(BoundedBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, int size){
        super(inputBuffer, outputBuffer, new CallableWithArguments<>() {
            BucketHistory bucketHistory = new PrecalculatedBucketHistory(size);

            @Override
            public Buckets call(Buckets input) {
                bucketHistory = bucketHistory.addNewBuckets(input);
                Buckets timeAveragedBuckets = bucketHistory.getTimeAveragedBuckets();
                return timeAveragedBuckets;
            }
        });
    }

}
