
public class Buckets {
    private double[] buckets;
    private final int width;

    public Buckets(int width) {
        this.width = width;
        buckets = new double[width];
    }

    void clear() {
        for(int i = 0; i< width; i++){
            buckets[i] = 0;
        }
    }

    void fill(int x, double value) {
        buckets[x]+=value;
    }

    double getValue(int i) {
        return buckets[i];
    }
}