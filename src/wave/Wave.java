package wave;

import frequency.Frequency;
import sound.SampleRate;

public class Wave {

    private final SampleRate sampleRate;
    private final Frequency frequency;
    private final double frequencyAngleComponent;

    private Wave(Frequency frequency, SampleRate sampleRate) {
        this.sampleRate = sampleRate;
        this.frequency = frequency;
        frequencyAngleComponent = frequency.getValue() * 2.0 * Math.PI;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public double getAmplitude(long sampleCount) {
        return getAmplitude(sampleRate.asTime(sampleCount).getValue());
    }

    private double getAmplitude(double timeInSeconds) {
        double angle = timeInSeconds * frequencyAngleComponent;
        return Math.sin(angle);
    }

    public static double getAmplitude(SampleRate sampleRate, Frequency frequency, Long sampleCount){
        double sampleTime = sampleRate.asTime(sampleCount).getValue();

        double frequencyAngleComponent = frequency.getValue() * 2.0 * Math.PI;
        double angle = sampleTime * frequencyAngleComponent;

        return Math.sin(angle);
    }

    public double getAmplitudePrecalculated(double timeAndAngleComponent) {
        double angle = timeAndAngleComponent * frequency.getValue();
        return Math.sin(angle);
    }

}
