package notes.state;

import frequency.*;
import notes.Note;
import notes.envelope.SimpleDeterministicEnvelope;
import notes.envelope.functions.DeterministicFunction;
import notes.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import sound.SampleTicker;
import time.TimeInSeconds;

public class NoteManager {
    private final SampleTicker sampleTicker;
    private final SampleRate sampleRate;

    private final DeterministicFunction envelopeFunction;

    private NoteState noteState;

    public NoteManager(SampleTicker sampleTicker, SampleRate sampleRate) {
        this.sampleTicker = sampleTicker;
        this.sampleRate = sampleRate;
        noteState = new NoteState();
        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.25, new TimeInSeconds(0.3));
    }

    public void addNote(Frequency frequency) {
        Note note = new Note(frequency,
                new SimpleDeterministicEnvelope(sampleTicker.getExpectedTickCount(),
                                                sampleRate,
                                                envelopeFunction)
        );

        synchronized (this) {
            noteState = noteState.addNote(note);
        }
    }




    protected NoteState getNoteState(long sampleCount) {
        synchronized(this) {
            noteState = noteState.update(sampleCount);
            return noteState;
        }
    }
}