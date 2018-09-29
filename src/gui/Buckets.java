package gui;

import java.util.*;
import java.util.Map.Entry;

public class Buckets {
    final private HashMap<Integer, Double> bucketsData;

    public Buckets() {
        bucketsData = new HashMap<>();
    }

    public Buckets(Entry<Integer, Double> bucketEntry) {
        this();
        Integer x = bucketEntry.getKey();
        Double value = bucketEntry.getValue();
        put(x, value);
    }

    public Buckets(Set<Entry<Integer, Double>> Entries) {
        this();
        for(Entry<Integer, Double> Entry : Entries){
            Integer x = Entry.getKey();
            put(x, getValue(x) + Entry.getValue());
        }
    }

    private void put(int x, double value) {
        bucketsData.put(x, value);
    }

    double getValue(int i) {
        try {
            return bucketsData.get(i);
        }
        catch(NullPointerException e){
            return 0.0;
        }
    }

    public Buckets add(Buckets otherBuckets) {
        Set<Entry<Integer, Double>> newEntries = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            Double value = Entry.getValue();
            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, value));
        }

        iterator = otherBuckets.iterator();
        while(iterator.hasNext()){
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            Double value = Entry.getValue();
            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, value));
        }
        return new Buckets(newEntries);
    }

    public Set<Entry<Integer, Double>> findMaxima() {
        Set<Entry<Integer, Double>> maxima = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();

            Double center = Entry.getValue();
            Double left = getValue(x-1);
            Double right = getValue(x+1);

            if(center>left && center>right){
                maxima.add(new AbstractMap.SimpleImmutableEntry<>(x, center));
            }
        }
        return maxima;
    }

    public Buckets averageBuckets(int averagingWidth) {
        Set<Entry<Integer, Double>> newEntries = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            Double value = Entry.getValue();

            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, value));

            for(int i = 1; i< averagingWidth; i++) {
                double residue = value * (averagingWidth - i) / averagingWidth;
                newEntries.add(new AbstractMap.SimpleImmutableEntry(x - i, residue));
                newEntries.add(new AbstractMap.SimpleImmutableEntry(x + i, residue));
            }
        }
        return new Buckets(newEntries);
    }

    public Buckets multiply(double v) {
        Set<Entry<Integer, Double>> newEntries = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            Double value = Entry.getValue();
            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, v * value));
        }
        return new Buckets(newEntries);
    }

    public Iterator<Entry<Integer, Double>> iterator() {
        return bucketsData.entrySet().iterator();
    }

    public Buckets clip(int start, int end) {
        Set<Entry<Integer, Double>> newEntries = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()) {
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            if (x < start || x >= end) {
                continue;
            }
            Double value = Entry.getValue();
            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, value));
        }
        return new Buckets(newEntries);
    }

    public Buckets clip(int max) {
        Set<Entry<Integer, Double>> newEntries = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()) {
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            Double value = Entry.getValue();
            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, Math.min(max, value)));
        }
        return new Buckets(newEntries);
    }

    public Buckets subtract(Buckets buckets) {
        return add(buckets.multiply(-1));
    }

    public Buckets add(double a) {
        Set<Entry<Integer, Double>> newEntries = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            Double value = Entry.getValue();
            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, a + value));
        }
        return new Buckets(newEntries);
    }

    public Buckets divide(Buckets otherBuckets) {
        return multiply(otherBuckets.reciprocal());
    }

    private Buckets reciprocal() {
        Set<Entry<Integer, Double>> newEntries = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            Double value = Entry.getValue();
            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, 1. / value));
        }
        return new Buckets(newEntries);
    }

    public Buckets multiply(Buckets otherBuckets) {
        Set<Entry<Integer, Double>> newEntries = new HashSet<>();

        Iterator<Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Entry<Integer, Double> Entry = iterator.next();
            Integer x = Entry.getKey();
            Double value = Entry.getValue();
            if(value == 0.0){
                continue;
            }
            Double value2 = otherBuckets.getValue(x);
            if(value2 == 0.0){
                continue;
            }
            double newValue = value * value2;
            newEntries.add(new AbstractMap.SimpleImmutableEntry(x, newValue));
        }
        return new Buckets(newEntries);
    }
}