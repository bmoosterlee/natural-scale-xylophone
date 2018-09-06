package notes;

import notes.envelope.Envelope;

public class Note {
    final double frequency;
    final Envelope envelope;

    public Note(double frequency, Envelope envelope){
        this.frequency = frequency;
        this.envelope = envelope;
    }
}
