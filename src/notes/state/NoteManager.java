package notes.state;

import frequency.*;
import main.BoundedBuffer;
import main.InputPort;
import notes.Note;
import notes.envelope.*;
import notes.envelope.functions.DeterministicFunction;
import notes.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import sound.SampleTicker;
import time.TimeInSeconds;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;

public class NoteManager {

    private NoteState noteState;
    long updatedToSample = -1;

    private InputPort<SimpleImmutableEntry<Long, Frequency>> newNotes;

    public NoteManager(SampleRate sampleRate, BoundedBuffer<SimpleImmutableEntry<Long, Frequency>> buffer) {
        newNotes = new InputPort<>(buffer);

        SampleTicker sampleTicker = new SampleTicker(sampleRate);
        sampleTicker.getTickObservable().add(this::tick);
        sampleTicker.start();

        noteState = new NoteState();
    }

    private void tick(Long sampleCount) {
        try {
            SimpleImmutableEntry<Long, Frequency> newNote = newNotes.consume();
            addNote(newNote.getKey(), newNote.getValue());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addNote(Long startTime, Frequency frequency) {
        Note note = new Note(frequency, startTime);

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