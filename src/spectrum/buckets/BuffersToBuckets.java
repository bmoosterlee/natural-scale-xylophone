package spectrum.buckets;

import component.*;
import component.buffer.*;

import java.util.*;

public class BuffersToBuckets extends PipeComponent<Pulse, Buckets> {

    public BuffersToBuckets(SimpleBuffer<Pulse> tickBuffer, Map<Integer, BoundedBuffer<AtomicBucket>> inputMap, SimpleBuffer<Buckets> outputBuffer) {
        super(tickBuffer, outputBuffer, toMethod(buildPipe(inputMap)));
    }

    public static CallableWithArguments<BoundedBuffer<Pulse>, BoundedBuffer<Buckets>> buildPipe(Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        return inputBuffer -> toBucketMap(inputBuffer, bufferMap)
            .performMethod(Buckets::new, "buffers to buckets - create buckets");
    }

    private static SimpleBuffer<Map<Integer, MemoizedBucket>> toBucketMap(BoundedBuffer<Pulse> input, Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        LinkedList<BoundedBuffer<Pulse>> frameTickBroadcast = new LinkedList<>(input.broadcast(bufferMap.size(), "buffers to buckets tick - broadcast"));
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
            output.put(index, input.get(index).performMethod(methods.get(index), "buffers to buckets - for each method application"));
        }
        return output;
    }

    public static <I, K, V> Map<I, BoundedBuffer<V>> forEach(Map<I, BoundedBuffer<K>> input, CallableWithArguments<K, V> method) {
        Map<I, BoundedBuffer<V>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(method, "buffers to buckets - for each single method application"));
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
        OutputPort<ImmutableMap<Integer, MemoizedBucket>> incompleteMapOutputPort = new OutputPort<>("buffers to buckets - incomplete map");
        BoundedBuffer<ImmutableMap<Integer, MemoizedBucket>> incompleteMap = incompleteMapOutputPort.getBuffer();
        try {
            incompleteMapOutputPort.produce(new ImmutableMap<>());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(Integer index : bufferMap.keySet()){
            LinkedList<SimpleBuffer<ImmutableMap<Integer, MemoizedBucket>>> incompleteMapBroadcast = new LinkedList<>(incompleteMap.broadcast(2, "buffers to buckets toBucketsMap2 - broadcast"));
            incompleteMap =
                incompleteMapBroadcast.poll()
                .pairWith(
                    incompleteMapBroadcast.poll()
                    .performMethod(in -> new Pulse(), "buffers to buckets - to pulse")
                    .performMethod(
                        Flusher.flush(bufferMap.get(index)), "buffers to buckets - flush")
                    .performMethod(input1 -> new MemoizedBucket(new CompositeBucket<>(input1)), "buffers to buckets - create bucket")
                    .performMethod(input2 -> new AbstractMap.SimpleImmutableEntry<>(index, input2), "buffers to buckets - pair indrx new entry"))
                .performMethod(input1 -> put(input1), "buffers to buckets - put entry");
        }
        return incompleteMap.performMethod(input1 -> input1.map, "buffers to buckets - extract Map");
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
