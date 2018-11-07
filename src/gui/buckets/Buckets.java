package gui.buckets;

import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;
import java.util.Map.Entry;

public class Buckets {
    private final Set<Integer> indices;
    private final Map<Integer, Bucket> bucketsData;

    public Buckets() {
        this(new HashSet<>(), new HashMap<>());
    }

    public Buckets(Set<Integer> indices, Map<Integer, Bucket> buckets) {
        this.indices = indices;
        this.bucketsData = buckets;
    }

    public Bucket getValue(int i) {
        return getBucketsData().get(i);
    }

    public Buckets add(Buckets otherBuckets) {
        Set<Integer> newIndices = new HashSet<>(getIndices());
        Map<Integer, Bucket> newEntries = new HashMap<>(getBucketsData());

        for(Integer index : otherBuckets.getIndices()){
            Bucket newBucket = otherBuckets.getValue(index);

            try {
                Bucket oldBucket = newEntries.get(index);
                newBucket = oldBucket.add(newBucket);

            } catch (NullPointerException e) {
                newIndices.add(index);
            }

            newEntries.put(index, newBucket);
        }

        Buckets newBuckets = new Buckets(newIndices, newEntries);

        return newBuckets;
    }

    public static Buckets add(Collection<Buckets> bucketsCollection) {
        Buckets newBuckets = new Buckets();

        for(Buckets buckets : bucketsCollection){
            newBuckets = newBuckets.add(buckets);
        }

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

    private Buckets memoize() {
        Map<Integer, Bucket> newMap = new HashMap<>();

        for(Integer index : indices){
            newMap.put(index, new MemoizedBucket(bucketsData.get(index)));
        }

        return new Buckets(indices, newMap);
    }

    Buckets transpose(int i) {
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        for(Integer index : getIndices()){
            int transposedIndex = index + i;

            newIndices.add(transposedIndex);
            newEntries.put(transposedIndex, getValue(index));
        }

        return new Buckets(newIndices, newEntries);
    }
}