import javafx.util.Pair;

import java.util.HashSet;
import java.util.Set;

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

    public Buckets(Pair<Integer, Double> bucketEntry, int length) {
        this(length);
        int x = bucketEntry.getKey();
        if (x >= 0 && x < getLength()) {
            put(x, bucketEntry.getValue());
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

    public Buckets add(Buckets otherBuckets) {
        Buckets newBuckets = new Buckets(getLength());
        for(int i = 0; i< getLength(); i++){
            newBuckets.put(i, getValue(i) + otherBuckets.getValue(i));
        }
        return newBuckets;
    }

    public int getLength() {
        return bucketsData.length;
    }

    public Set<Pair<Integer, Double>> findMaxima() {
        Set<Pair<Integer, Double>> maxima = new HashSet<>();
        for(int x = 0; x<getLength(); x++){
            double left = 0.;
            try{
                left = getValue(x-1);
            }
            catch(ArrayIndexOutOfBoundsException e){

            }
            double center = getValue(x);
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
        for(int x = 0; x< getLength(); x++) {
            averagedBuckets.fill(x, getValue(x));

            for(int i = 1; i< averagingWidth; i++) {
                double value = getValue(x) * (averagingWidth - i) / averagingWidth;
                try {
                    averagedBuckets.fill(x - i, value);
                } catch (ArrayIndexOutOfBoundsException e) {

                }
                try {
                    averagedBuckets.fill(x + i, value);
                } catch (ArrayIndexOutOfBoundsException e) {

                }
            }
        }
        return averagedBuckets;
    }

    public Buckets multiply(double v) {
        Buckets multipliedBuckets = new Buckets(getLength());
        for(int x = 0; x< getLength(); x++) {
            multipliedBuckets.put(x, v * getValue(x));
        }
        return multipliedBuckets;
    }

    public Buckets multiply(Buckets otherBuckets) {
        Buckets multipliedBuckets = new Buckets(getLength());
        for(int x = 0; x< getLength(); x++) {
            multipliedBuckets.put(x, getValue(x) * otherBuckets.getValue(x));
        }
        return multipliedBuckets;
    }

    public Buckets add(double v) {
        Buckets newBuckets = new Buckets(getLength());
        for(int x = 0; x< getLength(); x++) {
            newBuckets.put(x, getValue(x) + v);
        }
        return newBuckets;
    }
}