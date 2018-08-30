package main;

public class SampleRate {
    public final int sampleRate;

    public SampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public double asTime(long sampleCount) {
        return (double)sampleCount / sampleRate;
    }

    public long asSampleCount(double time) {
        return (long) (time * sampleRate);
    }
}