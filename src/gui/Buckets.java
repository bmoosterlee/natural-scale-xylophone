package gui;

import java.util.*;
import java.util.Map.Entry;

public class Buckets {
    //todo constantly put new harmonics in a harmonicsBuckets which is used over time.
    //todo copy the harmonicsBuckets when we want to use it somewhere else, but always keep adding.

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
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        Iterator<Entry<Integer, Bucket>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Bucket> Entry = iterator.next();
            Integer x = Entry.getKey();
            Bucket value = Entry.getValue();
            newIndices.add(x);
            newEntries.put(x, value);
        }

        iterator = otherBuckets.iterator();
        while(iterator.hasNext()){
            Entry<Integer, Bucket> Entry = iterator.next();
            Integer x = Entry.getKey();
            Bucket value = Entry.getValue();
            fill(newIndices, newEntries, x, value);
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
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        Iterator<Entry<Integer, Bucket>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Bucket> Entry = iterator.next();
            Integer x = Entry.getKey();
            Bucket bucket = Entry.getValue();

            fill(newIndices, newEntries, x, bucket);

            for(int i = 1; i< averagingWidth; i++) {
                double residue = bucket.volume * (averagingWidth - i) / averagingWidth;

                {
                    int residueIndex = x - i;

                    Bucket residueBucket = new Bucket(residue);
                    fill(newIndices, newEntries, residueIndex, residueBucket);
                }
                {
                    int residueIndex = x + i;

                    Bucket residueBucket = new Bucket(residue);
                    fill(newIndices, newEntries, residueIndex, residueBucket);
                }
            }
        }
        return new Buckets(newIndices, newEntries);
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
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        Iterator<Entry<Integer, Bucket>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Bucket> Entry = iterator.next();
            Integer x = Entry.getKey();
            Bucket bucket = Entry.getValue();

            newEntries.put(x, new Bucket(v * bucket.volume, bucket.frequencies, bucket.volumes));
        }
        return new Buckets(newIndices, newEntries);
    }

    public Iterator<Entry<Integer, Bucket>> iterator() {
        return bucketsData.entrySet().iterator();
    }

    public Buckets clip(int start, int end) {
        Set<Integer> newIndices = new HashSet<>();
        Map<Integer, Bucket> newEntries = new HashMap<>();

        Iterator<Entry<Integer, Bucket>> iterator = iterator();
        while(iterator.hasNext()) {
            Entry<Integer, Bucket> Entry = iterator.next();
            Integer x = Entry.getKey();

            if (x < start || x >= end) {
                continue;
            }

            Bucket bucket = Entry.getValue();
            newIndices.add(x);
            newEntries.put(x, bucket);
        }
        return new Buckets(newIndices, newEntries);
    }

    public Buckets subtract(Buckets buckets) {
        return add(buckets.multiply(-1));
    }

}