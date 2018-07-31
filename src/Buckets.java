
public class Buckets {
    private double[] bucketsData;

    public Buckets(int width) {
        bucketsData = new double[width];
    }

    void clear() {
        for(int i = 0; i< bucketsData.length; i++){
            bucketsData[i] = 0;
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
}