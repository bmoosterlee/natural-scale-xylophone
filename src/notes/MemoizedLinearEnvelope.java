package notes;

import main.SampleRate;

public class MemoizedLinearEnvelope extends Envelope{
    final static EnvelopeMemoizer ENVELOPE_MEMOIZER = new EnvelopeMemoizer();

    public MemoizedLinearEnvelope(long startingSampleCount, SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        super(startingSampleCount, sampleRate, ENVELOPE_MEMOIZER.get(sampleRate, amplitude, lengthInSeconds));
    }

}