package notes.state;

import frequency.*;
import notes.Note;
import notes.envelope.DeterministicEnvelope;
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
    long updatedToSample = -1;

    public NoteManager(SampleTicker sampleTicker, SampleRate sampleRate) {
        this.sampleTicker = sampleTicker;
        this.sampleRate = sampleRate;
        noteState = new NoteState();
        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.10, new TimeInSeconds(0.7));
    }

    public void addNote(Frequency frequency) {
        Note note = new Note<DeterministicEnvelope>(frequency,
                new SimpleDeterministicEnvelope(sampleTicker.getExpectedTickCount(),
                                                sampleRate,
                                                envelopeFunction)
        );

        synchronized (this) {
            noteState = noteState.addNote(note);
        }
    }




    public NoteState getNoteState(long sampleCount) {
        synchronized(this) {
            if(sampleCount>updatedToSample) {
                noteState = noteState.update(sampleCount);
                updatedToSample = sampleCount;
            }
            return noteState;
        }
    }
}