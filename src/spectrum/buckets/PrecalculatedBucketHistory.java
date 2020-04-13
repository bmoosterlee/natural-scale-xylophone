package spectrum.buckets;

import component.buffer.PipeCallable;

import java.util.LinkedList;

public class PrecalculatedBucketHistory {


    public static PipeCallable<Buckets, Buckets> build(int size){
        return new PipeCallable<>() {
            LinkedList<Buckets> harmonicsBucketsHistory = new LinkedList<>();
            Buckets timeAverage = new Buckets();
            double multiplier = 1. / size;

            @Override
            public Buckets call(Buckets input) {
                Buckets inputMultiplied = input.multiply(multiplier);

                harmonicsBucketsHistory.addLast(inputMultiplied);
                timeAverage = timeAverage.add(inputMultiplied);

                if (harmonicsBucketsHistory.size() == size) {
                    Buckets removed = harmonicsBucketsHistory.pollFirst();
                    timeAverage = timeAverage.subtract(removed);
                }

                return timeAverage;
            }

        };
    }
}