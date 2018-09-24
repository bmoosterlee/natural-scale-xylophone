package wave;

import frequency.Frequency;
import sound.SampleRate;

public class Wave {
    private final SampleRate sampleRate;
    private final Frequency frequency;
    private final double frequencyAngleComponent;

    public Wave(Frequency frequency, SampleRate sampleRate) {
        this.sampleRate = sampleRate;
        this.frequency = frequency;
        frequencyAngleComponent = frequency.getValue() * 2.0 * Math.PI;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public double getAmplitude(long sampleCount) {
        double angle = sampleRate.asTime(sampleCount).getValue() * frequencyAngleComponent;
        return Math.sin(angle);
    }
}
