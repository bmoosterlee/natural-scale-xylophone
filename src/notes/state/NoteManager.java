package notes.state;

import frequency.*;
import notes.Note;
import notes.envelope.PrecalculatedEnvelope;
import notes.envelope.SimpleDeterministicEnvelope;
import notes.envelope.functions.DeterministicFunction;
import notes.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import sound.SampleTicker;
import time.TimeInSeconds;
import wave.WaveState;

public class NoteManager {
    //todo move frequencyState elsewhere.
    private final SampleTicker sampleTicker;
    private final SampleRate sampleRate;

    private final DeterministicFunction envelopeFunction;

    private NoteState noteState;
    private FrequencyState frequencyState;
    private WaveState waveState;

    public NoteManager(SampleTicker sampleTicker, SampleRate sampleRate) {
        this.sampleTicker = sampleTicker;
        this.sampleRate = sampleRate;
        noteState = new NoteState();
        frequencyState = new SimpleFrequencyState();
        waveState = new WaveState(sampleRate);
        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.25, new TimeInSeconds(0.3));
    }

    public void addNote(Frequency frequency) {
        Note note = new Note(frequency,
                new SimpleDeterministicEnvelope(sampleTicker.getExpectedTickCount(),
                                                sampleRate,
                                                envelopeFunction)
        );

        synchronized (noteState) {
            noteState = noteState.addNote(note);
        }
    }

    public WaveState getWaveState(long sampleCount) {
        synchronized (noteState) {
            getFrequencyState(sampleCount);
            waveState = waveState.update(frequencyState.getFrequencies());
            waveState = waveState.update(sampleCount);
            return waveState;
        }
    }

    public FrequencyState getFrequencyState(long sampleCount) {
        synchronized (noteState) {
            getNoteState(sampleCount);
            frequencyState = frequencyState.update(noteState.getNotes());
            frequencyState = frequencyState.update(sampleCount);
            return frequencyState;
        }
    }

    private NoteState getNoteState(long sampleCount) {
        noteState = noteState.update(sampleCount);
        return noteState;
    }

}