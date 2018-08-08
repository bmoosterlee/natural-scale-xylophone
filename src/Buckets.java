import javafx.util.Pair;

import java.util.*;

public class Buckets {
    final private HashMap<Integer, Double> bucketsData;

    public Buckets() {
        bucketsData = new HashMap<>();
    }

    public Buckets(Buckets buckets) {
        this();
        Iterator<Map.Entry<Integer, Double>> iterator = buckets.iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            put(x, value);
        }
    }

    public Buckets(Pair<Integer, Double> bucketEntry) {
        this();
        int x = bucketEntry.getKey();
        put(x, bucketEntry.getValue());
    }

    void fill(int x, double value) {
        put(x, getValue(x) + value);
    }

    void put(int x, double value) {
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
        Buckets newBuckets = new Buckets();

        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newBuckets.put(x, value);
        }

        iterator = otherBuckets.iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            newBuckets.put(x, newBuckets.getValue(x) + value);
        }
        return newBuckets;
    }

    public int getLength() {
        return 0;
    }

    public Set<Pair<Integer, Double>> findMaxima() {
        Set<Pair<Integer, Double>> maxima = new HashSet<>();
        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
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
        Buckets averagedBuckets = new Buckets();
        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
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
        Buckets multipliedBuckets = new Buckets();
        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            Double value = pair.getValue();
            multipliedBuckets.put(x, v * value);
        }
        return multipliedBuckets;
    }

    public Iterator<Map.Entry<Integer, Double>> iterator() {
        return bucketsData.entrySet().iterator();
    }

    public Buckets clip(int start, int end) {
        Buckets newBuckets = new Buckets();
        Iterator<Map.Entry<Integer, Double>> iterator = iterator();
        while(iterator.hasNext()) {
            Map.Entry<Integer, Double> pair = iterator.next();
            Integer x = pair.getKey();
            if (x < start || x >= end) {
                break;
            }
            Double value = pair.getValue();
            newBuckets.put(x, value);
        }
        return newBuckets;
    }
}