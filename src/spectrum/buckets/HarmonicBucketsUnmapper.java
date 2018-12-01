package spectrum.buckets;

import component.*;
import component.buffer.*;

import java.util.*;

public class HarmonicBucketsUnmapper extends MethodPipeComponent<Pulse, Buckets> {

    public HarmonicBucketsUnmapper(SimpleBuffer<Pulse> tickBuffer, Map<Integer, BoundedBuffer<AtomicBucket>> inputMap, SimpleBuffer<Buckets> outputBuffer) {
        super(tickBuffer, outputBuffer, toMethod(buildPipe(inputMap)));
    }

    public static PipeCallable<BoundedBuffer<Pulse>, BoundedBuffer<Buckets>> buildPipe(Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        return inputBuffer -> toBucketMap(inputBuffer, bufferMap)
            .performMethod(Buckets::new, "harmonic buckets unmapper - create buckets");
    }

    private static SimpleBuffer<Map<Integer, MemoizedBucket>> toBucketMap(BoundedBuffer<Pulse> input, Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        LinkedList<BoundedBuffer<Pulse>> frameTickBroadcast = new LinkedList<>(input.broadcast(bufferMap.size(), "harmonic buckets unmapper - tick broadcast"));
        Map<Integer, BoundedBuffer<Pulse>> frameTickers = new HashMap<>();
        for (Integer index : bufferMap.keySet()) {
            frameTickers.put(index, frameTickBroadcast.poll());
        }

        Map<Integer, PipeCallable<Pulse, List<AtomicBucket>>> flushers = new HashMap<>();
        for (Integer index : bufferMap.keySet()) {
            flushers.put(index, Flusher.flush(bufferMap.get(index)));
        }

        return Unmapper.buildComponent(
                Mapper.forEach(
                    Mapper.forEach(
                        Mapper.forEach(frameTickers, flushers),
                        CompositeBucket::new),
                MemoizedBucket::new));
    }

    private static BoundedBuffer<Map<Integer, MemoizedBucket>> toBucketMap2(BoundedBuffer<Pulse> input, Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        OutputPort<ImmutableMap<Integer, MemoizedBucket>> incompleteMapOutputPort = new OutputPort<>("harmonic buckets unmapper - incomplete map");
        BoundedBuffer<ImmutableMap<Integer, MemoizedBucket>> incompleteMap = incompleteMapOutputPort.getBuffer();
        try {
            incompleteMapOutputPort.produce(new ImmutableMap<>());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(Integer index : bufferMap.keySet()){
            LinkedList<SimpleBuffer<ImmutableMap<Integer, MemoizedBucket>>> incompleteMapBroadcast = new LinkedList<>(incompleteMap.broadcast(2, "harmonic buckets unmapper - toBucketsMap2 broadcast"));
            incompleteMap =
                incompleteMapBroadcast.poll()
                .pairWith(
                    incompleteMapBroadcast.poll()
                    .performMethod(in -> new Pulse(), "harmonic buckets unmapper - to pulse")
                    .performMethod(
                        Flusher.flush(bufferMap.get(index)), "harmonic buckets unmapper - flush")
                    .performMethod(input1 -> new MemoizedBucket(new CompositeBucket<>(input1)), "harmonic buckets unmapper - create bucket")
                    .performMethod(input2 -> new AbstractMap.SimpleImmutableEntry<>(index, input2), "harmonic buckets unmapper - pair indrx new entry"))
                .performMethod(input1 -> put(input1), "harmonic buckets unmapper - put entry");
        }
        return incompleteMap.performMethod(input1 -> input1.map, "harmonic buckets unmapper - extract Map");
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

    public static <K, V> ImmutableMap<K, V> put(AbstractMap.SimpleImmutableEntry<ImmutableMap<K, V>, AbstractMap.SimpleImmutableEntry<K, V>> in){
        Map<K, V> newMap = new HashMap<>(in.getKey().map);
        newMap.put(in.getValue().getKey(), in.getValue().getValue());
        return new ImmutableMap<>(newMap);
    }

}
