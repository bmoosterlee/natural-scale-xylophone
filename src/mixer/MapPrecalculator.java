package mixer;

import component.buffer.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

class MapPrecalculator<I, K, V, A extends Packet<I>, B extends Packet<PrecalculatorOutputData<I, Set<K>, V>>> extends AbstractPipeComponent<I, PrecalculatorOutputData<I, Set<K>, V>, A, B> {
    private final Map<I, Set<K>> unfinishedData;
    private final PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator;
    private final BiFunction<V, V, V> adder;
    private final Callable<V> emptyFinishedDataBuilder;

    private final Map<I, V> finishedData;
    private PipeCallable<I, PrecalculatorOutputData<I, Set<K>, V>> transform;

    public MapPrecalculator(BoundedBuffer<I, A> inputBuffer, SimpleBuffer<PrecalculatorOutputData<I, Set<K>, V>, B> outputBuffer, PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator, Map<I, Set<K>> unfinishedData, BiFunction<V, V, V> adder, Callable<V> emptyFinishedDataBuilder) {
        super(inputBuffer.createInputPort(), outputBuffer.createOutputPort());
        this.calculator = calculator;
        this.unfinishedData = unfinishedData;
        this.adder = adder;
        this.emptyFinishedDataBuilder = emptyFinishedDataBuilder;
        finishedData = new HashMap<>();
        transform = input -> {
            Set<K> finalUnfinishedData = null;
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

    private Set<K> getUnfinishedData(I sampleCount) {
        Set<K> finalUnfinishedData;
        synchronized (unfinishedData) {
            if (unfinishedData.containsKey(sampleCount)) {
                finalUnfinishedData = unfinishedData.remove(sampleCount);
            } else {
                finalUnfinishedData = Collections.emptySet();
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
            Set<K> unfinishedItems;
            Set<K> unfinishedItemsInMap = unfinishedData.get(unfinishedKey);
            synchronized (unfinishedItemsInMap) {
                if(!unfinishedItemsInMap.isEmpty()) {
                    unfinishedItems = new HashSet<>(unfinishedItemsInMap);
                    unfinishedItemsInMap.clear();
                } else {
                    unfinishedItems = null;
                }
            }
            if(unfinishedItems!=null) {
                V finishedItem = null;
                for(K unfinishedItem : unfinishedItems) {
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
    }

    public static <I, K, V, A extends Packet<I>, B extends Packet<PrecalculatorOutputData<I, Set<K>, V>>> PipeCallable<BoundedBuffer<I, A>, BoundedBuffer<PrecalculatorOutputData<I, Set<K>, V>, B>> buildPipe(final Map<I, Set<K>> unfinishedData, PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator, BiFunction<V, V, V> adder, Callable<V> emptyFinishedDataBuilder) {
        return inputBuffer -> {
            SimpleBuffer<PrecalculatorOutputData<I, Set<K>, V>, B> outputBuffer = new SimpleBuffer<>(1, "precalculator - output");
            new TickRunningStrategy(new MapPrecalculator<>(inputBuffer, outputBuffer, calculator, unfinishedData, adder, emptyFinishedDataBuilder));
            return outputBuffer;
        };
    }


    @Override
    public Boolean isParallelisable() {
        return false;
    }
}
