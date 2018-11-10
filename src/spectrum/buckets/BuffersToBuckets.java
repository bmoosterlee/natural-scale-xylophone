package spectrum.buckets;

import component.*;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuffersToBuckets extends TickablePipeComponent<Pulse, Buckets> {

    public BuffersToBuckets(BoundedBuffer<Pulse> tickBuffer, Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap, BoundedBuffer<Buckets> bucketsBuffer) {
        super(tickBuffer, bucketsBuffer, toBuckets(bufferMap));
    }

    public static CallableWithArguments<Pulse, Buckets> toBuckets(Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        return new CallableWithArguments<>() {
            final Map<Integer, InputPort<AtomicBucket>> inputMap;

            {
                inputMap = new HashMap<>();
                for (Integer index : bufferMap.keySet()) {
                    inputMap.put(index, new InputPort<>(bufferMap.get(index)));
                }
            }

            private Buckets toBuckets() {
                try {
                    Set<Integer> indices = inputMap.keySet();
                    Map<Integer, List<AtomicBucket>> entryMap = new HashMap<>();

                    for (Integer index : indices) {
                        entryMap.put(index, inputMap.get(index).flush());
                    }

                    TimeKeeper timeKeeper = PerformanceTracker.startTracking("convert atomic buckets to composite");
                    Map<Integer, Bucket> compositeBucketMap = new HashMap<>();

                    for (Integer index : indices) {
                        compositeBucketMap.put(index, new MemoizedBucket(new CompositeBucket<>(entryMap.get(index))));
                    }
                    PerformanceTracker.stopTracking(timeKeeper);

                    return new Buckets(indices, compositeBucketMap);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Buckets call(Pulse input) {
                return toBuckets();
            }
        };
    }
}
