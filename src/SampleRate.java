public class SampleRate {
    final int sampleRate;

    public SampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    double asTime(long sampleCount) {
        return (double)sampleCount / sampleRate;
    }

    public long asSampleCount(double time) {
        return (long) (time * sampleRate);
    }
}