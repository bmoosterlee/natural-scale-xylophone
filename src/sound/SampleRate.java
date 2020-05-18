package sound;

import time.TimeInSeconds;

public class SampleRate {
    public final int sampleRate;

    public SampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public TimeInSeconds asTime(long sampleCount) {
        return new TimeInSeconds((double)sampleCount / sampleRate);
    }

    public double asTimeRaw(long sampleCount) {
        return ((double)sampleCount / sampleRate);
    }

    public long asSampleCount(TimeInSeconds time) {
        return (long) (time.getValue() * sampleRate);
    }

}
