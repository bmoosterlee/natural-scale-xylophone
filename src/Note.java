public class Note {

    private final Envelope envelope;
    private double frequency;
    private double frequencyPi;

    public Note(double frequency, long startingSampleCount, float sampleRate){
        this.envelope = new Envelope(startingSampleCount, sampleRate);
        this.frequency = frequency;
        this.frequencyPi = getFrequency() * 2.0 * Math.PI;
    }

    public double getAmplitude(long sampleCount) {
        double volume = getEnvelope().getVolume(sampleCount);
        double timeDifference = getEnvelope().getTimeDifference(sampleCount);
        double angle = timeDifference * frequencyPi;
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
