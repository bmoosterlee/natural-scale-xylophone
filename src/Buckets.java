import javafx.util.Pair;

import java.util.Arrays;

public class Buckets {
    final private double[] bucketsData;

    public Buckets(int width) {
        bucketsData = new double[width];
    }

    public Buckets(Buckets buckets) {
        this(buckets.getLength());
        for(int i = 0; i< getLength(); i++){
            put(i, buckets.getValue(i));
        }
    }

    void clear() {
        for(int i = 0; i< getLength(); i++){
            bucketsData[i] = 0.;
        }
    }

    void fill(int x, double value) {
        bucketsData[x]+=value;
    }

    void put(int x, double value) {
        bucketsData[x] = value;
    }

    double getValue(int i) {
        return bucketsData[i];
    }

    public Buckets add(Buckets buckets) {
        Buckets newBuckets = new Buckets(getLength());
        for(int i = 0; i< getLength(); i++){
            newBuckets.put(i, getValue(i) + buckets.getValue(i));
        }
        return newBuckets;
    }

    public int getLength() {
        return bucketsData.length;
    }

    public Pair<Integer, Double>[] sortBuckets() {
        Pair<Integer, Double>[] bucketPairs = new Pair[getLength()];
        for(int i = 0; i< getLength(); i++){
            bucketPairs[i] = new Pair(i, getValue(i));
        }
        Arrays.sort(bucketPairs, (Pair<Integer, Double> o1, Pair<Integer, Double> o2) -> {
            return -Double.compare(o1.getValue(), o2.getValue());
        });
        return bucketPairs;
    }
}