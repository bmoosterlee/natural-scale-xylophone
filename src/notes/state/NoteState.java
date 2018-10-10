package notes.state;

import notes.Note;
import notes.envelope.EnvelopeState;

import java.util.HashSet;
import java.util.Set;

public class NoteState {
    public final Set<Note> notes;

    public NoteState(){
        notes = new HashSet<>();
    }

    public NoteState(Set<Note> notes) {
        this.notes = notes;
    }

    public NoteState addNote(Note note) {
        if(note==null) throw new NullPointerException();

        Set<Note> newLiveNotes = new HashSet<>(notes);
        newLiveNotes.add(note);
        return new NoteState(newLiveNotes);
    }

    public Set<Note> getNotes() {
        return new HashSet<>(notes);
    }

    public NoteState removeNote(Note note) {
        if(note==null) throw new NullPointerException();

        Set<Note> newLiveNotes = new HashSet<>(notes);
        newLiveNotes.remove(note);
        return new NoteState(newLiveNotes);
    }

    public NoteState update(EnvelopeState envelopeState, long sampleCount) {
        Set<Note> newNotes = new HashSet<>();

        newNotes.removeAll(envelopeState.getDeadNotes(sampleCount));

        return new NoteState(newNotes);
    }
}