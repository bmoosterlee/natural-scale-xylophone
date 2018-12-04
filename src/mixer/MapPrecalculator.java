package mixer;

import component.buffer.*;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.ConcurrentHashMap;

class MapPrecalculator<I, K, V, A extends Packet<I>, B extends Packet<Set<V>>, C extends Packet<SimpleImmutableEntry<I, Set<K>>>>
        extends AbstractComponent {
    private final ConcurrentHashMap<I, Set<K>> unfinishedData;
    private final PipeCallable<SimpleImmutableEntry<I, K>, V> calculator;

    private final Map<I, Set<V>> finishedData;
    private PipeCallable<I, SimpleImmutableEntry<I, Set<K>>> transformUnfinished;
    private final InputPort<I, A> inputPort;
    private final OutputPort<SimpleImmutableEntry<I, Set<K>>, C> unfinishedOutputPort;
    private final OutputPort<Set<V>, B> finishedOutputPort;
    private final PipeCallable<I, Set<V>> transformFinished;

    private MapPrecalculator(
            BoundedBuffer<I, A> inputBuffer,
            SimpleBuffer<SimpleImmutableEntry<I, Set<K>>, C> unfinishedOutputBuffer,
            SimpleBuffer<Set<V>, B> finishedOutputBuffer,
            PipeCallable<SimpleImmutableEntry<I, K>, V> calculator,
            ConcurrentHashMap<I, Set<K>> unfinishedData) {
        inputPort = inputBuffer.createInputPort();
        unfinishedOutputPort = unfinishedOutputBuffer.createOutputPort();
        finishedOutputPort = finishedOutputBuffer.createOutputPort();
        this.calculator = calculator;
        this.unfinishedData = unfinishedData;
        finishedData = new HashMap<>();
        transformUnfinished = input -> {
            Set<K> finalUnfinishedData = null;
            try {
                finalUnfinishedData = getUnfinishedData(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new SimpleImmutableEntry<>(
                    input,
                    finalUnfinishedData);
        };
        transformFinished = input -> {
            Set<V> finishedDataUntilNow = null;
            try {
                finishedDataUntilNow = getFinishedData(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return finishedDataUntilNow;
        };
    }

    @Override
    protected Collection<BoundedBuffer<I, A>> getInputBuffers() {
        return Collections.singleton(inputPort.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer<?, ? extends Packet<? extends Object>>> getOutputBuffers() {
        return Arrays.asList(unfinishedOutputPort.getBuffer(), finishedOutputPort.getBuffer());
    }

    @Override
    protected void tick() {
        try {
            A key = inputPort.consume();
            unfinishedOutputPort.produce(
                    key.transform(transformUnfinished)
            );
            finishedOutputPort.produce(
                    key.transform(transformFinished)
            );

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
        while (inputPort.isEmpty() && unfinishedKeyIterator.hasNext()) {
            Map.Entry<I, Set<K>> unfinishedEntry = unfinishedKeyIterator.next();
            I unfinishedKey = unfinishedEntry.getKey();
            Set<K> unfinishedItemsInMap = unfinishedData.get(unfinishedKey);
            if(!unfinishedItemsInMap.isEmpty()) {
                Set<K> unfinishedItems = new HashSet<>(unfinishedData.get(unfinishedKey));
                unfinishedData.get(unfinishedKey).removeAll(unfinishedItems);
                calculate(unfinishedKey, unfinishedItems);
            }
        }
    }

    private void calculate(I unfinishedKey, Set<K> unfinishedItems) {
        Set<V> finishedItems = new HashSet<>();
        for(K unfinishedItem : unfinishedItems) {
            try {
                finishedItems.add(calculator.call(
                        new SimpleImmutableEntry<>(
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

    public static <I, K, V, A extends Packet<I>, C extends Packet<SimpleImmutableEntry<I, Set<K>>>, B extends Packet<Set<V>>>
        PipeCallable<BoundedBuffer<I, A>, SimpleImmutableEntry<BoundedBuffer<SimpleImmutableEntry<I, Set<K>>, C>, BoundedBuffer<Set<V>, B>>> buildPipe(
            final ConcurrentHashMap<I, Set<K>> unfinishedData,
            PipeCallable<SimpleImmutableEntry<I, K>, V> calculator) {
        return inputBuffer -> {
            SimpleBuffer<SimpleImmutableEntry<I, Set<K>>, C> unfinishedOutputBuffer =
                    new SimpleBuffer<>(1, "precalculator - unfinished output");
            SimpleBuffer<Set<V>, B> finishedOutputBuffer =
                    new SimpleBuffer<>(1, "precalculator - finished output");
            new TickRunningStrategy(
                    new MapPrecalculator<>(
                            inputBuffer,
                            unfinishedOutputBuffer,
                            finishedOutputBuffer,
                            calculator,
                            unfinishedData
                    ));
            return new SimpleImmutableEntry<>(unfinishedOutputBuffer, finishedOutputBuffer);
        };
    }


    @Override
    public Boolean isParallelisable() {
        return false;
    }
}
