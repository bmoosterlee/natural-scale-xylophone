package notes.envelope.functions;

import main.SampleRate;

public abstract class DeterministicFunction extends EnvelopeFunction {
    public final double totalTime;

    public DeterministicFunction(SampleRate sampleRate, double amplitude, double totalTime) {
        super(sampleRate, amplitude);
        this.totalTime = totalTime;
    }

}
