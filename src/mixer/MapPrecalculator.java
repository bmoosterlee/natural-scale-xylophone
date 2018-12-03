package mixer;

import component.buffer.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class MapPrecalculator<I, K, V, A extends Packet<I>, B extends Packet<PrecalculatorOutputData<I, Set<K>, Set<V>>>>
        extends AbstractPipeComponent<I, PrecalculatorOutputData<I, Set<K>, Set<V>>, A, B> {
    private final ConcurrentHashMap<I, Set<K>> unfinishedData;
    private final PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator;

    private final Map<I, Set<V>> finishedData;
    private PipeCallable<I, PrecalculatorOutputData<I, Set<K>, Set<V>>> transform;

    private MapPrecalculator(
            BoundedBuffer<I, A> inputBuffer,
            SimpleBuffer<PrecalculatorOutputData<I, Set<K>, Set<V>>, B> outputBuffer,
            PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator,
            ConcurrentHashMap<I, Set<K>> unfinishedData) {
        super(inputBuffer.createInputPort(), outputBuffer.createOutputPort());
        this.calculator = calculator;
        this.unfinishedData = unfinishedData;
        finishedData = new HashMap<>();
        transform = input -> {
            Set<K> finalUnfinishedData = null;
            try {
                finalUnfinishedData = getUnfinishedData(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Set<V> finishedDataUntilNow = null;
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

    private Set<V> getFinishedData(I key) {
        Set<V> finishedDataUntilNow;
        if (finishedData.containsKey(key)) {
            finishedDataUntilNow = finishedData.remove(key);
        } else {
            finishedDataUntilNow = Collections.emptySet();
        }
        return finishedDataUntilNow;
    }

    private Set<K> getUnfinishedData(I sampleCount) {
        Set<K> finalUnfinishedData;
        if (unfinishedData.containsKey(sampleCount)) {
            finalUnfinishedData = unfinishedData.remove(sampleCount);
        } else {
            finalUnfinishedData = Collections.emptySet();
        }
        return finalUnfinishedData;
    }

    private void calculateContinuously() {
        Iterator<Map.Entry<I, Set<K>>> unfinishedKeyIterator = unfinishedData.entrySet().iterator();
        while (input.isEmpty() && unfinishedKeyIterator.hasNext()) {
            Map.Entry<I, Set<K>> unfinishedEntry = unfinishedKeyIterator.next();
            Set<K> unfinishedItemsInMap = unfinishedEntry.getValue();
            if(!unfinishedItemsInMap.isEmpty()) {
                I unfinishedKey = unfinishedEntry.getKey();
                Set<K> unfinishedItems = new HashSet<>(unfinishedData.get(unfinishedKey));
                unfinishedItemsInMap.removeAll(unfinishedItems);
                calculate(unfinishedKey, unfinishedItems);
            }
        }
    }

    private void calculate(I unfinishedKey, Set<K> unfinishedItems) {
        Set<V> finishedItems = new HashSet<>();
        for(K unfinishedItem : unfinishedItems) {
            try {
                finishedItems.add(calculator.call(
                        new AbstractMap.SimpleImmutableEntry<>(
                                unfinishedKey,
                                unfinishedItem)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (finishedData.containsKey(unfinishedKey)) {
                finishedData.get(
                        unfinishedKey)
                        .addAll(finishedItems);
            } else {
                finishedData.put(
                        unfinishedKey,
                        finishedItems
                );
            }
        }
    }

    public static <I, K, V, A extends Packet<I>, B extends Packet<PrecalculatorOutputData<I, Set<K>, Set<V>>>>
        PipeCallable<BoundedBuffer<I, A>, BoundedBuffer<PrecalculatorOutputData<I, Set<K>, Set<V>>, B>> buildPipe(
            final ConcurrentHashMap<I, Set<K>> unfinishedData,
            PipeCallable<AbstractMap.SimpleImmutableEntry<I, K>, V> calculator) {
        return inputBuffer -> {
            SimpleBuffer<PrecalculatorOutputData<I, Set<K>, Set<V>>, B> outputBuffer =
                    new SimpleBuffer<>(1, "precalculator - output");
            new TickRunningStrategy(
                    new MapPrecalculator<>(
                            inputBuffer,
                            outputBuffer,
                            calculator,
                            unfinishedData
                    ));
            return outputBuffer;
        };
    }


    @Override
    public Boolean isParallelisable() {
        return false;
    }
}
