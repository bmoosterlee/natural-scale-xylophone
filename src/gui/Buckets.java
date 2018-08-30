package gui;

import javafx.util.Pair;

import java.util.*;

public class Buckets {
    final private HashMap<Integer, Double> bucketsData;

    public Buckets() {
        bucketsData = new HashMap<>();
    }

    public Buckets(Pair<Integer, Double> bucketEntry) {
        this();
        Integer x = bucketEntry.getKey();
        Double value = bucketEntry.getValue();
        put(x, value);
    }

    public Buckets(Set<Pair<Integer, Double>> pairs) {
        this();
        for(Pair<Integer, Double> pair : pairs){
            Integer x = pair.getKey();
            put(x, getValue(x) + pair.getValue());
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
        Set<Pair<Integer, Double>> newPairs = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newPairs.add(new Pair(x, value));
        }

        iterator = otherBuckets.iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newPairs.add(new Pair(x, value));
        }
        return new Buckets(newPairs);
    }

    public Set<Pair<Integer, Double>> findMaxima() {
        Set<Pair<Integer, Double>> maxima = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();

            Double center = pair.getValue();
            Double left = getValue(x-1);
            Double right = getValue(x+1);

            if(center>left && center>right){
                maxima.add(new Pair<>(x, center));
            }
        }
        return maxima;
    }

    public Buckets averageBuckets(int averagingWidth) {
        Set<Pair<Integer, Double>> newPairs = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();

            newPairs.add(new Pair(x, value));

            for(int i = 1; i< averagingWidth; i++) {
                double residue = value * (averagingWidth - i) / averagingWidth;
                newPairs.add(new Pair(x - i, residue));
                newPairs.add(new Pair(x + i, residue));
            }
        }
        return new Buckets(newPairs);
    }

    public Buckets multiply(double v) {
        Set<Pair<Integer, Double>> newPairs = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newPairs.add(new Pair(x, v * value));
        }
        return new Buckets(newPairs);
    }

    public Iterator<Map.Entry<Integer, Double>> iterator() {
        return bucketsData.entrySet().iterator();
    }

    public Buckets clip(int start, int end) {
        Set<Pair<Integer, Double>> newPairs = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()) {
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            if (x < start || x >= end) {
                continue;
            }
            Double value = pair.getValue();
            newPairs.add(new Pair(x, value));
        }
        return new Buckets(newPairs);
    }

    public Buckets clip(int max) {
        Set<Pair<Integer, Double>> newPairs = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()) {
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newPairs.add(new Pair(x, Math.min(max, value)));
        }
        return new Buckets(newPairs);
    }

    public Buckets subtract(Buckets buckets) {
        return add(buckets.multiply(-1));
    }

    public Buckets add(double a) {
        Set<Pair<Integer, Double>> newPairs = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newPairs.add(new Pair(x, a + value));
        }
        return new Buckets(newPairs);
    }

    public Buckets divide(Buckets otherBuckets) {
        return multiply(otherBuckets.reciprocal());
    }

    private Buckets reciprocal() {
        Set<Pair<Integer, Double>> newPairs = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newPairs.add(new Pair(x, 1. / value));
        }
        return new Buckets(newPairs);
    }

    public Buckets multiply(Buckets otherBuckets) {
        Set<Pair<Integer, Double>> newPairs = new HashSet<>();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            Double value2 = otherBuckets.getValue(x);
            newPairs.add(new Pair(x, value * value2));
        }
        return new Buckets(newPairs);
    }
}