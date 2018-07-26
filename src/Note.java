public class Note {

    private final Envelope envelope;
    private double frequency;

    public Note(double frequency, long startingSampleCount, float sampleRate){
        this.frequency = frequency;
        this.envelope = new Envelope(startingSampleCount, sampleRate);
    }

    public double getAmplitude(long sampleCount) {
        double volume = getEnvelope().getVolume(sampleCount);
        double timeDifference = getEnvelope().getTimeDifference(sampleCount);
        double angle = timeDifference * getFrequency() * 2.0 * Math.PI;
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
