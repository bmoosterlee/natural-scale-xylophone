package gui.buckets;

public class BucketsAverager {
    int averagingWidth;
    double[] multipliers;

    public BucketsAverager(int averagingWidth) {
        this.averagingWidth = averagingWidth;

        double[] multipliers = new double[averagingWidth];
        for (int i = 1; i < averagingWidth; i++) {
            multipliers[i] = ((double) averagingWidth - i) / averagingWidth;
        }
        this.multipliers = multipliers;
    }
}