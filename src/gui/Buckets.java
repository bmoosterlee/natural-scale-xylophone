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
        Set<Integer> newIndices = new HashSet<>(indices);
        Map<Integer, Bucket> newEntries = new HashMap<>(bucketsData);

        for(Integer x : otherBuckets.indices){
            fill(newIndices, newEntries, x, otherBuckets.getValue(x));
        }
        return new Buckets(newIndices, newEntries);
    }

    public Buckets findMaxima() {
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> maxima = new HashMap<>();

        Iterator<Entry<Integer, Bucket>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Bucket> Entry = iterator.next();
            Integer x = Entry.getKey();

            Bucket center = Entry.getValue();
            Double centerValue = center.volume;
            Double left;
            try {
                left = getValue(x - 1).volume;
            }
            catch(NullPointerException e){
                left = centerValue;
            }
            Double right;
            try {
                right = getValue(x + 1).volume;
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
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("average buckets");

        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        double[] multipliers = bucketsAverager.multipliers;
        int averagingWidth = bucketsAverager.averagingWidth;

        for(Integer x : indices){
            Double volume = getValue(x).volume;

            for(int i = 1; i< averagingWidth; i++) {
                Bucket residueBucket = new Bucket(volume * multipliers[i]);

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

        Buckets buckets = add(new Buckets(newIndices, newEntries));
        PerformanceTracker.stopTracking(timeKeeper);

        return buckets;
    }

    protected static void fill(Set<Integer> newIndices, Map<Integer, Bucket> entries, Integer x, Bucket bucket) {
        newIndices.add(x);
        try {
            entries.put(x, entries.get(x).add(bucket));
        } catch (NullPointerException e) {
            entries.put(x, bucket);
        }
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