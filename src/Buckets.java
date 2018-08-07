import javafx.util.Pair;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Buckets {
    final private double[] bucketsData;

    public Buckets(int width) {
        bucketsData = new double[width];
    }

    public Buckets(Buckets buckets) {
        this(buckets.getLength());
        Iterator<Pair<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Pair<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            put(x, value);
        }
    }

    public Buckets(Pair<Integer, Double> bucketEntry, int length) {
        this(length);
        int x = bucketEntry.getKey();
        if (x >= 0 && x < getLength()) {
            put(x, bucketEntry.getValue());
        }
    }

    void fill(int x, double value) {
        put(x, getValue(x) + value);
    }

    void put(int x, double value) {
        bucketsData[x] = value;
    }

    double getValue(int i) {
        return bucketsData[i];
    }

    public Buckets add(Buckets otherBuckets) {
        Buckets newBuckets = new Buckets(getLength());
        Iterator<Pair<Integer, Double>> iterator = otherBuckets.iterator();
        while(iterator.hasNext()){
            Pair<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newBuckets.put(x, getValue(x) + value);
        }
        return newBuckets;
    }

    public int getLength() {
        return bucketsData.length;
    }

    public Set<Pair<Integer, Double>> findMaxima() {
        Set<Pair<Integer, Double>> maxima = new HashSet<>();
        Iterator<Pair<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Pair<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double center = pair.getValue();
            double left = 0.;
            try{
                left = getValue(x-1);
            }
            catch(ArrayIndexOutOfBoundsException e){

            }
            double right = 0.;
            try{
                right = getValue(x+1);
            }
            catch(ArrayIndexOutOfBoundsException e){

            }
            if(center>left && center>right){
                maxima.add(new Pair<>(x, center));
            }
        }
        return maxima;
    }

    Buckets averageBuckets(int averagingWidth) {
        Buckets averagedBuckets = new Buckets(getLength());
        Iterator<Pair<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Pair<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();

            averagedBuckets.fill(x, value);

            for(int i = 1; i< averagingWidth; i++) {
                double residue = value * (averagingWidth - i) / averagingWidth;
                try {
                    averagedBuckets.fill(x - i, residue);
                } catch (ArrayIndexOutOfBoundsException e) {

                }
                try {
                    averagedBuckets.fill(x + i, residue);
                } catch (ArrayIndexOutOfBoundsException e) {

                }
            }
        }
        return averagedBuckets;
    }

    public Buckets multiply(double v) {
        Buckets multipliedBuckets = new Buckets(getLength());
        Iterator<Pair<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Pair<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            multipliedBuckets.put(x, v * value);
        }
        return multipliedBuckets;
    }

    public Iterator<Pair<Integer, Double>> iterator() {
        return new Iterator<Pair<Integer, Double>>() {
            int counter = 0;

            @Override
            public boolean hasNext() {
                return counter<bucketsData.length;
            }

            @Override
            public Pair<Integer, Double> next() {
                Pair<Integer, Double> pair = new Pair<>(counter, bucketsData[counter]);
                counter++;
                return pair;
            }
        };
    }
}