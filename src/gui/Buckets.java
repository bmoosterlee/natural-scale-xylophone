package gui;

import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;
import java.util.Map.Entry;

public class Buckets {
    protected final Set<Integer> indices;
    protected final Map<Integer, Bucket> bucketsData;

    public Buckets() {
        indices = new HashSet<>();
        bucketsData = new HashMap<>();
    }

    public Buckets(Set<Integer> indices, Map<Integer, Bucket> buckets) {
        this.indices = indices;
        this.bucketsData = buckets;
    }

    public Bucket getValue(int i) {
        return bucketsData.get(i);
    }

    public Buckets add(Buckets otherBuckets) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("add buckets setup");
        Set<Integer> newIndices = new HashSet<>(indices);
        Map<Integer, Bucket> newEntries = new HashMap<>(bucketsData);

        for(Integer x : otherBuckets.indices){
            Bucket value = otherBuckets.getValue(x);
            PerformanceTracker.stopTracking(timeKeeper);
            fill(newIndices, newEntries, x, value);
            timeKeeper = PerformanceTracker.startTracking("add buckets setup");
        }
        Buckets newBuckets = new Buckets(newIndices, newEntries);
        PerformanceTracker.stopTracking(timeKeeper);
        return newBuckets;
    }

    public Buckets findMaxima() {
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> maxima = new HashMap<>();

        Iterator<Entry<Integer, Bucket>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Bucket> Entry = iterator.next();
            Integer x = Entry.getKey();

            Bucket center = Entry.getValue();
            Double centerValue = center.getVolume();
            Double left;
            try {
                left = getValue(x - 1).getVolume();
            }
            catch(NullPointerException e){
                left = centerValue;
            }
            Double right;
            try {
                right = getValue(x + 1).getVolume();
            }
            catch(NullPointerException e){
                right = centerValue;
            }

            if(centerValue>left && centerValue>right){
                newIndices.add(x);
                maxima.put(x, center);
            }
        }
        return new Buckets(newIndices, maxima);
    }

    public Buckets averageBuckets(int averagingWidth) {
        return averageBuckets(new BucketsAverager(averagingWidth));
    }

    public Buckets averageBuckets(BucketsAverager bucketsAverager) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("average buckets setup");
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        double[] multipliers = bucketsAverager.multipliers;
        int averagingWidth = bucketsAverager.averagingWidth;
        PerformanceTracker.stopTracking(timeKeeper);

        for(Integer x : indices){
            timeKeeper = PerformanceTracker.startTracking("average buckets get volume");
            Double volume = getValue(x).getVolume();
            PerformanceTracker.stopTracking(timeKeeper);

            for(int i = 1; i< averagingWidth; i++) {
                timeKeeper = PerformanceTracker.startTracking("average buckets multiply bucket");
                AtomicBucket residueBucket = new AtomicBucket(volume * multipliers[i]);
                PerformanceTracker.stopTracking(timeKeeper);

                {
                    int residueIndex = x - i;

                    fill(newIndices, newEntries, residueIndex, residueBucket);
                }
                {
                    int residueIndex = x + i;

                    fill(newIndices, newEntries, residueIndex, residueBucket);
                }
            }
        }

        timeKeeper = PerformanceTracker.startTracking("average buckets construct new buckets");
        Buckets newBuckets = new Buckets(newIndices, newEntries);
        PerformanceTracker.stopTracking(timeKeeper);

        Buckets buckets = add(newBuckets);
        return buckets;
    }

    protected static void fill(Set<Integer> newIndices, Map<Integer, Bucket> entries, Integer x, Bucket bucket) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("fill");
        newIndices.add(x);
        Bucket newBucket;

        try {
            Bucket oldBucket = entries.get(x);
            PerformanceTracker.stopTracking(timeKeeper);

            newBucket = oldBucket.add(bucket);

            timeKeeper = PerformanceTracker.startTracking("fill");
        } catch (NullPointerException e) {
            newBucket = bucket;
        }

        entries.put(x, newBucket);
        PerformanceTracker.stopTracking(timeKeeper);
    }

    public Buckets multiply(double v) {
        Map<Integer, Bucket> newEntries = new HashMap<>();

        for(Integer x : indices){
            newEntries.put(x, getValue(x).multiply(v));
        }
        return new Buckets(indices, newEntries);
    }

    public Iterator<Entry<Integer, Bucket>> iterator() {
        return bucketsData.entrySet().iterator();
    }

    public Buckets clip(int start, int end) {
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        for(Integer x : indices){
            if (x < start || x >= end) {
                continue;
            }

            newIndices.add(x);
            newEntries.put(x, getValue(x));
        }
        return new Buckets(newIndices, newEntries);
    }

    public Buckets subtract(Buckets buckets) {
        return add(buckets.multiply(-1));
    }

}