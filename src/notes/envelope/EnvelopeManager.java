package notes.envelope;

import notes.Note;
import notes.state.NoteManager;
import notes.state.NoteState;
import sound.SampleRate;

import java.util.Set;

public class EnvelopeManager {
    private final NoteManager noteManager;

    EnvelopeState envelopeState;
    long updatedToSample = -1;

    public EnvelopeManager(NoteManager noteManager, SampleRate sampleRate){
        this.noteManager = noteManager;
        envelopeState = new EnvelopeState(sampleRate);
    }

    public EnvelopeState getEnvelopeState(long sampleCount){
        synchronized (this) {
            if(sampleCount>updatedToSample) {
                NoteState noteState = noteManager.getNoteState(envelopeState, sampleCount);
                envelopeState = envelopeState.update(noteState.getNotes());
                envelopeState = envelopeState.update(sampleCount);
                
                Set<Note> deadNotes = envelopeState.getDeadNotes(sampleCount);
                for (Note note : deadNotes) {
                    noteManager.removeNote(note);
                }
                updatedToSample = sampleCount;
            }
            return envelopeState;
        }
    }
}
