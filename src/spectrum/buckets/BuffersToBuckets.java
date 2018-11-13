package spectrum.buckets;

import component.*;
import component.buffer.*;
import component.utilities.RunningOutputComponent;
import component.utilities.RunningPipeComponent;

import java.util.*;

public class BuffersToBuckets extends RunningPipeComponent<Pulse, Buckets> {

    public BuffersToBuckets(BoundedBuffer<Pulse> tickBuffer, Map<Integer, BoundedBuffer<AtomicBucket>> inputMap, SimpleBuffer<Buckets> outputBuffer) {
        super(tickBuffer, outputBuffer, build(inputMap));
    }

    public static CallableWithArguments<Pulse, Buckets> build(Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        return new CallableWithArguments<>() {
            private OutputPort<Pulse> methodInputPort;
            private InputPort<Buckets> methodOutputPort;

            {
                BoundedBuffer<Pulse> methodInput = new SimpleBuffer<>(1, "BuffersToBuckets - input");
                methodInputPort = methodInput.createOutputPort();

                BoundedBuffer<Map<Integer, MemoizedBucket>> bucketMap = toBucketMap(methodInput, bufferMap);

                methodOutputPort =
                    bucketMap
                    .performMethod(Buckets::new).createInputPort();
            }

            @Override
            public Buckets call(Pulse input) {
                try {
                    methodInputPort.produce(input);

                    Buckets result = methodOutputPort.consume();

                    return result;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    private static SimpleBuffer<Map<Integer, MemoizedBucket>> toBucketMap(BoundedBuffer<Pulse> input, Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        LinkedList<BoundedBuffer<Pulse>> frameTickBroadcast = new LinkedList<>(input.broadcast(bufferMap.size()));
        Map<Integer, BoundedBuffer<Pulse>> frameTickers = new HashMap<>();
        for (Integer index : bufferMap.keySet()) {
            frameTickers.put(index, frameTickBroadcast.poll());
        }

        Map<Integer, CallableWithArguments<Pulse, List<AtomicBucket>>> flushers = new HashMap<>();
        for (Integer index : bufferMap.keySet()) {
            flushers.put(index, Flusher.flush(bufferMap.get(index)));
        }

        return collect(
                toInputPorts(
                        forEach(
                                forEach(frameTickers, flushers),
                                input1 -> new MemoizedBucket(new CompositeBucket<>(input1)))));
    }

    public static <I, K, V> Map<I, BoundedBuffer<V>> forEach(Map<I, BoundedBuffer<K>> input, Map<I, CallableWithArguments<K, V>> methods) {
        Map<I, BoundedBuffer<V>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(methods.get(index)));
        }
        return output;
    }

    public static <I, K, V> Map<I, BoundedBuffer<V>> forEach(Map<I, BoundedBuffer<K>> input, CallableWithArguments<K, V> method) {
        Map<I, BoundedBuffer<V>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(method));
        }
        return output;
    }

    public static <I, K> Map<I, InputPort<K>> toInputPorts(Map<I, BoundedBuffer<K>> input){
        Map<I, InputPort<K>> map = new HashMap<>();
        for (I index : input.keySet()) {
            map.put(index, input.get(index).createInputPort());
        }
        return map;
    }

    public static <I, K> SimpleBuffer<Map<I, K>> collect(Map<I, InputPort<K>> input){
        return RunningOutputComponent.buildOutputBuffer(
            () -> {
                try {
                    Map<I, K> map = new HashMap<>();
                    for (I index : input.keySet()) {
                        map.put(index, input.get(index).consume());
                    }

                    return map;
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
                return null;
            },
        1,
        "buffers to buckets - collect");
    }

    private static BoundedBuffer<Map<Integer, MemoizedBucket>> toBucketMap2(BoundedBuffer<Pulse> input, Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        BoundedBuffer<ImmutableMap<Integer, MemoizedBucket>> incompleteMap = new SimpleBuffer<>(1, "toBucketsMap2");
        try {
            incompleteMap.createOutputPort().produce(new ImmutableMap<>());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(Integer index : bufferMap.keySet()){
            LinkedList<SimpleBuffer<ImmutableMap<Integer, MemoizedBucket>>> broadcast = new LinkedList<>(incompleteMap.broadcast(2));
            incompleteMap =
                broadcast.poll()
                .pairWith(
                    broadcast.poll()
                    .performMethod(in -> new Pulse())
                    .performMethod(
                        Flusher.flush(bufferMap.get(index)))
                    .performMethod(input1 -> new MemoizedBucket(new CompositeBucket<>(input1)))
                    .performMethod(input2 -> new AbstractMap.SimpleImmutableEntry<>(index, input2)))
                .performMethod(input1 -> put(input1));
        }
        return incompleteMap.performMethod(input1 -> input1.map);
    }

    private static class ImmutableMap<K, V>{
        Map<K, V> map;

        public ImmutableMap(){
            map = new HashMap<>();
        }

        public ImmutableMap(Map<K, V> map) {
            this.map = map;
        }

        public ImmutableMap put(K key, V value){
            Map<K, V> newMap = new HashMap<>(map);
            newMap.put(key, value);
            return new ImmutableMap<>(newMap);
        }

        public Map<K, V> getMap(){
            return map;
        }
    }

    public static <K, V> ImmutableMap put(AbstractMap.SimpleImmutableEntry<ImmutableMap<K, V>, AbstractMap.SimpleImmutableEntry<K, V>> in){
        Map<K, V> newMap = new HashMap<>(in.getKey().map);
        newMap.put(in.getValue().getKey(), in.getValue().getValue());
        return new ImmutableMap<>(newMap);
    }

}
