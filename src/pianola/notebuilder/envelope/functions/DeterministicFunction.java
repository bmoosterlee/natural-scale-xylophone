package pianola.notebuilder.envelope.functions;

import sound.SampleRate;
import time.TimeInSeconds;

public abstract class DeterministicFunction extends EnvelopeFunction {
    public final TimeInSeconds totalTime;

    DeterministicFunction(SampleRate sampleRate, double amplitude, TimeInSeconds totalTime) {
        super(sampleRate, amplitude);
        this.totalTime = totalTime;
    }

}
