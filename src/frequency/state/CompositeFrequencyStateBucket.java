package frequency.state;

import notes.Note;
import notes.envelope.Envelope;

import java.util.HashSet;
import java.util.Set;

public class CompositeFrequencyStateBucket implements FrequencyStateBucket {

    final Set<Note> notes;
    final Envelope envelope;

    public CompositeFrequencyStateBucket(Set<Note> notes, Envelope envelope) {
        this.notes = notes;
        this.envelope = envelope;
    }

    public CompositeFrequencyStateBucket addNote(Note note) {
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.add(note);

        Envelope envelope = note.getEnvelope();

        Envelope newEnvelope;
        try {
            newEnvelope = this.envelope.add(envelope);
        }
        catch(NullPointerException e){
            newEnvelope = envelope;
        }

        return new CompositeFrequencyStateBucket(newNotes, newEnvelope);
    }

    public CompositeFrequencyStateBucket removeNote(Note note) {
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.remove(note);

        return new CompositeFrequencyStateBucket(newNotes, envelope);
    }

    public Double getVolume(long sampleCount) {
        return envelope.getVolume(sampleCount);
    }

    public CompositeFrequencyStateBucket update(long sampleCount) {
        Set<Note> newNotes = new HashSet<>(notes);
        Envelope newEnvelope = envelope.update(sampleCount);

        return new CompositeFrequencyStateBucket(newNotes, newEnvelope);
    }
}
