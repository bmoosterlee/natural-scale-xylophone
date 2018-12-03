package mixer;

import component.buffer.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

class MapPrecalculator<I, K, V, A extends Packet<I>, B extends Packet<PrecalculatorOutputData<I, K, V>>> extends AbstractPipeComponent<I, PrecalculatorOutputData<I, K, V>, A, B> {
    private final Map<I, K> unfinishedData;
    private final PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator;
    private final BiFunction<V, V, V> adder;
    private final Callable<K> emptyUnfinishedDataBuilder;
    private final Callable<V> emptyFinishedDataBuilder;

    private final Map<I, V> finishedData;
    private PipeCallable<I, PrecalculatorOutputData<I, K, V>> transform;

    public MapPrecalculator(BoundedBuffer<I, A> inputBuffer, SimpleBuffer<PrecalculatorOutputData<I, K, V>, B> outputBuffer, PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator, Map<I, K> unfinishedData, BiFunction<V, V, V> adder, Callable<K> emptyUnfinishedDataBuilder, Callable<V> emptyFinishedDataBuilder) {
        super(inputBuffer.createInputPort(), outputBuffer.createOutputPort());
        this.calculator = calculator;
        this.unfinishedData = unfinishedData;
        this.adder = adder;
        this.emptyUnfinishedDataBuilder = emptyUnfinishedDataBuilder;
        this.emptyFinishedDataBuilder = emptyFinishedDataBuilder;
        finishedData = new HashMap<>();
        transform = input -> {
            K finalUnfinishedData = null;
            try {
                finalUnfinishedData = getUnfinishedData(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            V finishedDataUntilNow = null;
            try {
                finishedDataUntilNow = getFinishedData(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new PrecalculatorOutputData<>(
                    input,
                    finalUnfinishedData,
                    finishedDataUntilNow);
        };

    }

    @Override
    protected void tick() {
        try {
            A key = input.consume();
            output.produce(
                    key.transform(transform));

            calculateContinuously();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private V getFinishedData(I key) throws Exception {
        V finishedDataUntilNow;
        if (finishedData.containsKey(key)) {
            finishedDataUntilNow = finishedData.remove(key);
        } else {
            finishedDataUntilNow = emptyFinishedDataBuilder.call();
        }
        return finishedDataUntilNow;
    }

    private K getUnfinishedData(I sampleCount) throws Exception {
        K finalUnfinishedData;
        synchronized (unfinishedData) {
            if (unfinishedData.containsKey(sampleCount)) {
                finalUnfinishedData = unfinishedData.remove(sampleCount);
            } else {
                finalUnfinishedData = emptyUnfinishedDataBuilder.call();
            }
        }
        return finalUnfinishedData;
    }

    private void calculateContinuously() {
        Iterator<I> unfinishedKeyIterator;
        synchronized (unfinishedData) {
            unfinishedKeyIterator = new HashSet<>(unfinishedData.keySet()).iterator();
        }
        while (input.isEmpty() && unfinishedKeyIterator.hasNext()) {
            I unfinishedKey = unfinishedKeyIterator.next();
            K unfinishedItem;
            synchronized (unfinishedData) {
                unfinishedItem = unfinishedData.remove(unfinishedKey);
            }
            if(unfinishedItem!=null) {
                V finishedItem = null;
                try {
                    finishedItem = calculator.call(
                            new AbstractMap.SimpleImmutableEntry<>(
                                    unfinishedKey,
                                    unfinishedItem));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (finishedData.containsKey(unfinishedKey)) {
                    finishedData.put(
                            unfinishedKey,
                            adder.apply(
                                    finishedData.remove(unfinishedKey),
                                    finishedItem));
                } else {
                    finishedData.put(
                            unfinishedKey,
                            finishedItem
                    );
                }
            }
        }
    }

    public static <I, K, V, A extends Packet<I>, B extends Packet<PrecalculatorOutputData<I, K, V>>> PipeCallable<BoundedBuffer<I, A>, BoundedBuffer<PrecalculatorOutputData<I, K, V>, B>> buildPipe(final Map<I, K> unfinishedData, PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator, BiFunction<V, V, V> adder, Callable<K> emptyUnfinshedDataBuilder, Callable<V> emptyFinishedDataBuilder) {
        return inputBuffer -> {
            SimpleBuffer<PrecalculatorOutputData<I, K, V>, B> outputBuffer = new SimpleBuffer<>(1, "precalculator - output");
            new TickRunningStrategy(new MapPrecalculator<>(inputBuffer, outputBuffer, calculator, unfinishedData, adder, emptyUnfinshedDataBuilder, emptyFinishedDataBuilder));
            return outputBuffer;
        };
    }


    @Override
    public Boolean isParallelisable() {
        return false;
    }
}
