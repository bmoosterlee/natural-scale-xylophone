public class Note {

    private final Envelope envelope;
    private final double frequency;
    private final double frequencyPi;
    private final double startingTimeAngleComponent;

    public Note(double frequency, long startingSampleCount, float sampleRate){
        envelope = new Envelope(startingSampleCount, sampleRate);
        this.frequency = frequency;
        frequencyPi = getFrequency() * 2.0 * Math.PI;
        startingTimeAngleComponent = getEnvelope().getStartingTime() * frequencyPi;
    }

    public double getAmplitude(long sampleCount, double volume) {
        double angle = getEnvelope().getTimeAsDouble(sampleCount) * frequencyPi - startingTimeAngleComponent;
        return (Math.sin(angle) * volume);
    }

    public double getVolume(long sampleCount){
        return getEnvelope().getVolume(sampleCount);
    }

    public double getFrequency() {
        return frequency;
    }

    public Envelope getEnvelope() {
        return envelope;
    }
}
