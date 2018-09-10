package notes;

public class Wave {
    private final Frequency frequency;
    private final double frequencyAngleComponent;

    public Wave(Frequency frequency) {
        this.frequency = frequency;
        frequencyAngleComponent = frequency.getValue() * 2.0 * Math.PI;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public double getAmplitude(double time) {
        double angle = time * frequencyAngleComponent;
        return Math.sin(angle);
    }
}
