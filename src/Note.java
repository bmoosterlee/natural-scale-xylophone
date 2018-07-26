public class Note {

    private final Envelope envelope;
    private double frequency;

    public Note(double frequency, long startingSampleCount, float sampleRate){
        this.frequency = frequency;
        this.envelope = new Envelope(startingSampleCount, sampleRate);
    }

    public double getAmplitude(long sampleCount) {
        double volume = envelope.getVolume(sampleCount);
        double timeDifference = envelope.getTimeDifference(sampleCount);
        double angle = timeDifference * getFrequency() * 2.0 * Math.PI;
        return (Math.sin(angle) * volume);
    }

    public double getVolume(long sampleCount){
        return envelope.getVolume(sampleCount);
    }

    public double getFrequency() {
        return frequency;
    }

}
