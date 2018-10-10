package notes.state;

import frequency.*;
import notes.Note;
import notes.envelope.*;
import notes.envelope.functions.DeterministicFunction;
import notes.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import sound.SampleTicker;
import time.TimeInSeconds;

public class NoteManager {

    private final SampleTicker sampleTicker;

    private NoteState noteState;
    long updatedToSample = -1;

    public NoteManager(SampleTicker sampleTicker) {
        this.sampleTicker = sampleTicker;
        noteState = new NoteState();
    }

    public void addNote(Frequency frequency) {
        Note note = new Note(frequency, sampleTicker.getExpectedTickCount());

        synchronized (this) {
            noteState = noteState.addNote(note);
        }
    }

    public void removeNote(Note note){
        synchronized (this){
            noteState = noteState.removeNote(note);
        }
    }


    public NoteState getNoteState(EnvelopeState envelopeState, long sampleCount) {
        synchronized(this) {
            if(sampleCount>updatedToSample) {
                noteState = noteState.update(envelopeState, sampleCount);
                updatedToSample = sampleCount;
            }
            return noteState;
        }
    }

    public NoteState getNoteState(long sampleCount) {
        synchronized(this) {
            if(sampleCount>updatedToSample) {
                updatedToSample = sampleCount;
            }
            return noteState;
        }
    }
}