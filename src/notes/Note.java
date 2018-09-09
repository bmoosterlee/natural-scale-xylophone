package notes;

import notes.envelope.Envelope;

public class Note {
    final Frequency frequency;
    final Envelope envelope;

    public Note(Frequency frequency, Envelope envelope){
        this.frequency = frequency;
        this.envelope = envelope;
    }
}
