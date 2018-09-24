package notes.envelope.functions;

import sound.SampleRate;
import time.TimeInSeconds;

public abstract class DeterministicFunction extends EnvelopeFunction {
    public final TimeInSeconds totalTime;

    public DeterministicFunction(SampleRate sampleRate, double amplitude, TimeInSeconds totalTime) {
        super(sampleRate, amplitude);
        this.totalTime = totalTime;
    }

}
