package notes;

import main.SampleRate;

public class MemoizedLinearEnvelope extends Envelope{
    final static EnvelopeMemoizer ENVELOPE_MEMOIZER = new EnvelopeMemoizer();
    final Envelope envelope;

    public MemoizedLinearEnvelope(long startingSampleCount, SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        super(startingSampleCount, sampleRate);
        envelope = ENVELOPE_MEMOIZER.get(sampleRate, amplitude, lengthInSeconds);
    }

    @Override
    public double getVolume(double timeDifference) {
        return envelope.getVolume(timeDifference);
    }
}