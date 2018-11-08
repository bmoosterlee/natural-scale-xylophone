package gui.buckets;

import component.CallableWithArguments;
import component.InputPort;
import component.OutputPort;
import component.PipeComponent;

import java.util.AbstractMap;
import java.util.LinkedList;

public class PrecalculatedBucketHistory implements BucketHistory {
    private final int size;
    private final LinkedList<Buckets> history;

    private final double multiplier;
    private final Buckets timeAverage;
    private OutputPort<Buckets> multiplierOutputPort;
    private InputPort<Buckets> multiplierInputPort;


    public PrecalculatedBucketHistory(int size) {
        this(size, new LinkedList<>(), 1. / size, new Buckets());
    }

    private PrecalculatedBucketHistory(int size, LinkedList<Buckets> history, double multiplier, Buckets timeAverage) {
        this.size = size;
        this.history = history;
        this.multiplier = multiplier;
        this.timeAverage = timeAverage;

        AbstractMap.SimpleImmutableEntry<OutputPort<Buckets>, InputPort<Buckets>> addNewBucketsMultiplier = PipeComponent.methodToComponentPorts(input -> input.multiply(multiplier), 1, "buckets history - multiply");
        multiplierOutputPort = addNewBucketsMultiplier.getKey();
        multiplierInputPort = addNewBucketsMultiplier.getValue();
    }

    @Override
    public BucketHistory addNewBuckets(Buckets newBuckets) {
        try {
            multiplierOutputPort.produce(newBuckets);
            Buckets preparedNewBuckets = multiplierInputPort.consume();

            LinkedList<Buckets> newHistory = new LinkedList<>(history);
            Buckets newTimeAverage = timeAverage;
            if (history.size() >= size) {
                Buckets removed = newHistory.pollFirst();
                newTimeAverage = newTimeAverage.subtract(removed);
            }
            newHistory.addLast(preparedNewBuckets);
            newTimeAverage = newTimeAverage.add(preparedNewBuckets);

            return new PrecalculatedBucketHistory(size, newHistory, multiplier, newTimeAverage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Buckets getTimeAveragedBuckets() {
        return timeAverage;
    }

}