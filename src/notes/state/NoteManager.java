package notes.state;

import main.SampleRate;
import notes.Frequency;
import notes.Note;
import notes.envelope.PrecalculatedEnvelope;
import notes.envelope.SimpleDeterministicEnvelope;
import notes.envelope.functions.DeterministicFunction;
import notes.envelope.functions.LinearFunctionMemoizer;

public class NoteManager {
    //todo move frequencyState elsewhere.
    private final SampleTicker sampleTicker;
    private NoteState noteState;
    private FrequencyState frequencyState;
    private WaveState waveState;
    DeterministicFunction envelopeFunction;
    private SampleRate sampleRate;

    public NoteManager(SampleTicker sampleTicker, SampleRate sampleRate) {
        this.sampleTicker = sampleTicker;
        this.sampleRate = sampleRate;
        noteState = new NoteState();
        // frequencyState = new SimpleFrequencyState();
        frequencyState = new CompositeFrequencyState();
        waveState = new WaveState(sampleRate);
        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.05, 0.4);
    }

    public void addNote(Frequency frequency) {
        Note note = new Note(frequency, new PrecalculatedEnvelope(
                new SimpleDeterministicEnvelope(sampleTicker.getExpectedSampleCount(),
                                                sampleRate,
                                                envelopeFunction)));

        synchronized (noteState) {
            noteState = noteState.addNote(note);
        }
    }

    public NoteSnapshot getSnapshot(long sampleCount) {
        synchronized (noteState) {
            noteState = noteState.update(sampleCount);
            frequencyState = frequencyState.update(noteState.getNotes());
            frequencyState = frequencyState.update(sampleCount);
            waveState = waveState.update(frequencyState.getFrequencies());
            waveState = waveState.update(sampleCount);
            return new NoteSnapshot(noteState, frequencyState, waveState);
        }
    }
}