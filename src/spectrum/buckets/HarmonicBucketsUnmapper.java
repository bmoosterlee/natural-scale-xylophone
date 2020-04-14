package spectrum.buckets;

import component.*;
import component.buffer.*;

import java.util.*;

public class HarmonicBucketsUnmapper {

    public static <A extends Packet<Pulse>, B extends Packet<Buckets>> PipeCallable<BoundedBuffer<Pulse, A>, BoundedBuffer<Buckets, B>> buildPipe(Map<Integer, BoundedBuffer<AtomicBucket, SimplePacket<AtomicBucket>>> bufferMap) {
        return inputBuffer -> toBucketMap(inputBuffer, bufferMap)
            .performMethod(((PipeCallable<Map<Integer, MemoizedBucket>, Buckets>) Buckets::new), "harmonic buckets unmapper - create buckets");
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
}
