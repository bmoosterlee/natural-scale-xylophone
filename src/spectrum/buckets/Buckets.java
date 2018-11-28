package spectrum.buckets;

import frequency.Frequency;
import mixer.state.VolumeState;
import spectrum.SpectrumWindow;

import java.util.*;
import java.util.Map.Entry;

public class Buckets {
    private final Set<Integer> indices;
    private final Map<Integer, ? extends Bucket> bucketsData;

    public Buckets() {
        this(new HashSet<>(), new HashMap<>());
    }

    public Buckets(Set<Integer> indices, Map<Integer, ? extends Bucket> buckets) {
        this.indices = indices;
        this.bucketsData = buckets;
    }

    public Buckets(Map<Integer, ? extends Bucket> map) {
        this(map.keySet(), map);
    }

    public Buckets(VolumeState volumeState, SpectrumWindow spectrumWindow){
        Map<Frequency, Double> volumes = volumeState.volumes;
        Set<Frequency> keys = volumes.keySet();

        indices = new HashSet<>();
        HashMap<Integer, AtomicBucket> buckets = new HashMap<>();

        for(Frequency frequency : keys){
            int x = spectrumWindow.getX(frequency);

            indices.add(x);
            buckets.put(x, new AtomicBucket(frequency, volumes.get(frequency)));
        }

        bucketsData = buckets;
    }

    public Bucket getValue(int i) {
        return bucketsData.get(i);
    }

    public Buckets add(Buckets otherBuckets) {
        Set<Integer> newIndices = new HashSet<>(getIndices());
        Map<Integer, Bucket> newEntries = new HashMap<>(bucketsData);

        for(Integer index : otherBuckets.getIndices()){
            Bucket oldBucket = bucketsData.get(index);
            Bucket otherBucket = otherBuckets.getValue(index);

            if(oldBucket==null) {
                newIndices.add(index);
                newEntries.put(index, otherBucket);
            }
            else {
                Bucket newBucket = oldBucket.add(otherBucket);
                newEntries.put(index, newBucket);
            }
        }

        return new Buckets(newIndices, newEntries);
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

        Iterator<? extends Entry<Integer, ? extends Bucket>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, ? extends Bucket> Entry = iterator.next();
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

    public Iterator<? extends Entry<Integer, ? extends Bucket>> iterator() {
        return bucketsData.entrySet().iterator();
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