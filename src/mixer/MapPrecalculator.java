package mixer;

import component.buffer.*;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

class MapPrecalculator<I, K, V> extends AbstractPipeComponent<I, PrecalculatorOutputData<I, K, V>> {
    private final Map<I, K> unfinishedData;
    private final PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator;
    private final BiFunction<V, V, V> adder;
    private final Callable<K> emptyUnfinshedDataBuilder;
    private final Callable<V> emptyFinishedDataBuilder;

    final Map<I, V> finishedData;

    public MapPrecalculator(BoundedBuffer<I> inputBuffer, SimpleBuffer<PrecalculatorOutputData<I, K, V>> outputBuffer, PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator, Map<I, K> unfinishedData, BiFunction<V, V, V> adder, Callable<K> emptyUnfinshedDataBuilder, Callable<V> emptyFinishedDataBuilder) {
        super(inputBuffer.createInputPort(), outputBuffer.createOutputPort());
        this.calculator = calculator;
        this.unfinishedData = unfinishedData;
        this.adder = adder;
        this.emptyUnfinshedDataBuilder = emptyUnfinshedDataBuilder;
        this.emptyFinishedDataBuilder = emptyFinishedDataBuilder;
        finishedData = new HashMap<>();
    }

    @Override
    protected void tick() {
        try {
            I key = input.consume();
            K finalUnfinishedData = getUnfinishedData(key);
            V finishedDataUntilNow = getFinishedData(key);

            output.produce(
                    new PrecalculatorOutputData<>(
                            key,
                            finalUnfinishedData,
                            finishedDataUntilNow));

            calculateContinuously();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
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
                finalUnfinishedData = emptyUnfinshedDataBuilder.call();
            }
        }
        return finalUnfinishedData;
    }

    private void calculateContinuously() {
        Iterator<I> unfinishedKeyIterator = unfinishedData.keySet().iterator();
        while (input.isEmpty() && unfinishedKeyIterator.hasNext()) {
            I unfinishedKey = unfinishedKeyIterator.next();
            K unfinishedItem;
            synchronized (unfinishedData) {
                unfinishedItem = unfinishedData.remove(unfinishedKey);
            }
            if(unfinishedItem!=null) {
                V finishedItem = calculator.call(
                        new AbstractMap.SimpleImmutableEntry<>(
                                unfinishedKey,
                                unfinishedItem));
                if(finishedData.containsKey(unfinishedKey)) {
                    finishedData.put(
                            unfinishedKey,
                            adder.apply(
                                    finishedData.get(unfinishedKey),
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

    public static <I, K, V> PipeCallable<BoundedBuffer<I>, BoundedBuffer<PrecalculatorOutputData<I, K, V>>> buildPipe(final Map<I, K> unfinishedData, PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator, BiFunction<V, V, V> adder, Callable<K> emptyUnfinshedDataBuilder, Callable<V> emptyFinishedDataBuilder) {
        return inputBuffer -> {
            SimpleBuffer<PrecalculatorOutputData<I, K, V>> outputBuffer = new SimpleBuffer<>(1, "precalculator - output");
            new TickRunningStrategy(new MapPrecalculator<>(inputBuffer, outputBuffer, calculator, unfinishedData, adder, emptyUnfinshedDataBuilder, emptyFinishedDataBuilder));
            return outputBuffer;
        };
    }

    @Override
    public Boolean isParallelisable() {
        return false;
    }
}
