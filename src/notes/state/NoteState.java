package notes.state;

import notes.Note;

import java.util.HashSet;
import java.util.Set;

public class NoteState {
    //todo write a construction which makes sure notes is updated and removes dead deterministic notes
    //todo we could use sampleCount in getSnapshot, and return the updated list. The only issue is that new notes won't
    //todo be added yet.
    public final Set<Note> notes;

    public NoteState(){
        notes = new HashSet<>();
    }

    public NoteState(Set<Note> notes) {
        this.notes = notes;
    }

    public NoteState addNote(Note note) {
        Set<Note> newLiveNotes = new HashSet<>(notes);
        newLiveNotes.add(note);
        return new NoteState(newLiveNotes);
    }

    public NoteState update(long sampleCount) {
        Set<Note> newNotes = new HashSet();
        for(Note note : notes){
            if(!note.isDead(sampleCount)){
                newNotes.add(note);
            }
        }
        return new NoteState(newNotes);
    }

    public Set<Note> getNotes() {
        return new HashSet<>(notes);
    }
}