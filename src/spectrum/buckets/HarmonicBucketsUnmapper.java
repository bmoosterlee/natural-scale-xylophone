package spectrum.buckets;

import component.*;
import component.buffer.*;

import java.util.*;

public class HarmonicBucketsUnmapper {

    public static <A extends Packet<Pulse>, B extends Packet<Buckets>> PipeCallable<BoundedBuffer<Pulse, A>, BoundedBuffer<Buckets, B>> buildPipe(Map<Integer, BoundedBuffer<AtomicBucket, SimplePacket<AtomicBucket>>> bufferMap) {
        return inputBuffer -> toBucketMap(inputBuffer, bufferMap)
            .performMethod(Buckets::new, "harmonic buckets unmapper - create buckets");
    }

    private static <A extends Packet<Pulse>> SimpleBuffer<Map<Integer, MemoizedBucket>, SimplePacket<Map<Integer, MemoizedBucket>>> toBucketMap(BoundedBuffer<Pulse, A> input, Map<Integer, BoundedBuffer<AtomicBucket, SimplePacket<AtomicBucket>>> bufferMap) {
        LinkedList<BoundedBuffer<Pulse, A>> frameTickBroadcast = new LinkedList<>(input.broadcast(bufferMap.size(), "harmonic buckets unmapper - tick broadcast"));
        Map<Integer, BoundedBuffer<Pulse, A>> frameTickers = new HashMap<>();
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

    private static <A extends Packet<Pulse>, B extends Packet<Map<Integer, MemoizedBucket>>, C extends Packet<AtomicBucket>, D extends Packet<ImmutableMap<Integer, MemoizedBucket>>> BoundedBuffer<Map<Integer, MemoizedBucket>, B> toBucketMap2(BoundedBuffer<Pulse, A> input, Map<Integer, BoundedBuffer<AtomicBucket, C>> bufferMap) {
        OutputPort<ImmutableMap<Integer, MemoizedBucket>, D> incompleteMapOutputPort = new OutputPort<>("harmonic buckets unmapper - incomplete map");
        BoundedBuffer<ImmutableMap<Integer, MemoizedBucket>, D> incompleteMap = incompleteMapOutputPort.getBuffer();
        try {
            incompleteMapOutputPort.produce((D) new SimplePacket<>(new ImmutableMap<Integer, MemoizedBucket>()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(Integer index : bufferMap.keySet()){
            LinkedList<SimpleBuffer<ImmutableMap<Integer, MemoizedBucket>, D>> incompleteMapBroadcast = new LinkedList<>(incompleteMap.broadcast(2, "harmonic buckets unmapper - toBucketsMap2 broadcast"));
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
