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
}