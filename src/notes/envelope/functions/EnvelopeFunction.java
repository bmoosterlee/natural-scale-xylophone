package notes.envelope.functions;

import main.SampleRate;

public abstract class EnvelopeFunction {
    SampleRate sampleRate;
    protected double amplitude;

    public EnvelopeFunction(SampleRate sampleRate, double amplitude) {
        this.sampleRate = sampleRate;
        this.amplitude = amplitude;
    }

    public double getVolume(double timeDifference) {
        if (timeDifference < 0) {
            return 0;
        }

        return getVolume2(timeDifference);
    }

    protected abstract double getVolume2(double timeDifference);

}