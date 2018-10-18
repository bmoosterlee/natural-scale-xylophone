package gui.buckets;

import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;
import java.util.Map.Entry;

public class Buckets {
    private final Set<Integer> indices;
    private final Map<Integer, Bucket> bucketsData;

    public Buckets() {
        indices = new HashSet<>();
        bucketsData = new HashMap<>();
    }

    public Buckets(Set<Integer> indices, Map<Integer, Bucket> buckets) {
        this.indices = indices;
        this.bucketsData = buckets;
    }

    public Bucket getValue(int i) {
        return getBucketsData().get(i);
    }

    public Buckets add(Buckets otherBuckets) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("add buckets setup");
        Set<Integer> newIndices = new HashSet<>(getIndices());
        Map<Integer, Bucket> newEntries = new HashMap<>(getBucketsData());

        for(Integer x : otherBuckets.getIndices()){
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

    public static void fill(Set<Integer> newIndices, Map<Integer, Bucket> entries, Integer x, Bucket bucket) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("fill");
        newIndices.add(x);
        Bucket newBucket;

        try {
            Bucket oldBucket = entries.get(x);
            PerformanceTracker.stopTracking(timeKeeper);

            newBucket = new PrecalculatedBucket(oldBucket.add(bucket));

            timeKeeper = PerformanceTracker.startTracking("fill");
        } catch (NullPointerException e) {
            newBucket = bucket;
        }

        entries.put(x, newBucket);
        PerformanceTracker.stopTracking(timeKeeper);
    }

    public Buckets multiply(double v) {
        Map<Integer, Bucket> newEntries = new HashMap<>();

        for(Integer x : getIndices()){
            newEntries.put(x, getValue(x).multiply(v));
        }
        return new Buckets(getIndices(), newEntries);
    }

    public Iterator<Entry<Integer, Bucket>> iterator() {
        return getBucketsData().entrySet().iterator();
    }

    public Buckets clip(int start, int end) {
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        for(Integer x : getIndices()){
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

    public Set<Integer> getIndices() {
        return indices;
    }

    private Map<Integer, Bucket> getBucketsData() {
        return bucketsData;
    }

    public Buckets precalculate() {
        Map<Integer, Bucket> newMap = new HashMap<>();

        for(Integer index : indices){
            newMap.put(index, new PrecalculatedBucket(bucketsData.get(index)));
        }

        return new Buckets(indices, newMap);
    }
}