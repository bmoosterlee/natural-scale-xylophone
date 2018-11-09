package spectrum.buckets;

import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;
import component.Pulse;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuffersToBuckets implements Runnable {

    private final InputPort<Pulse> tickInput;
    private final Map<Integer, InputPort<AtomicBucket>> inputMap;
    private final OutputPort<Buckets> bucketsOutput;

    public BuffersToBuckets(Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap, BoundedBuffer<Pulse> tickBuffer, BoundedBuffer<Buckets> bucketsBuffer){
        inputMap = new HashMap<>();
        for(Integer index : bufferMap.keySet()){
            inputMap.put(index, new InputPort<>(bufferMap.get(index)));
        }

        tickInput = new InputPort<>(tickBuffer);
        bucketsOutput = new OutputPort<>(bucketsBuffer);

        start();
    }

    private void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    private void tick() {
        try {
            tickInput.consume();

            Set<Integer> indices = inputMap.keySet();
            Map<Integer, List<AtomicBucket>> entryMap = new HashMap<>();

            for(Integer index : indices) {
                entryMap.put(index, inputMap.get(index).flush());
            }

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("convert atomic buckets to composite");
            Map<Integer, Bucket> compositeBucketMap = new HashMap<>();

            for(Integer index : indices) {
                compositeBucketMap.put(index, new MemoizedBucket(new CompositeBucket<>(entryMap.get(index))));
            }
            PerformanceTracker.stopTracking(timeKeeper);

            Buckets newBuckets = new Buckets(indices, compositeBucketMap);

            bucketsOutput.produce(newBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
