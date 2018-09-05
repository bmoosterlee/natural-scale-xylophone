package notes;

public class Note {
    final double frequency;
    final Envelope envelope;

    public Note(double frequency, Envelope envelope){
        this.frequency = frequency;
        this.envelope = envelope;
    }
}
