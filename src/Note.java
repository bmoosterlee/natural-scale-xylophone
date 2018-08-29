public class Note {

    private final Envelope envelope;
    private final double frequency;

    public Note(double frequency, long startingSampleCount, SampleRate sampleRate){
        envelope = new Envelope(startingSampleCount, sampleRate);
        this.frequency = frequency;
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
