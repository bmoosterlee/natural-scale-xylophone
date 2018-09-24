package notes.envelope.functions;

import sound.SampleRate;
import time.TimeInSeconds;

public abstract class EnvelopeFunction {
    SampleRate sampleRate;
    protected double amplitude;

    public EnvelopeFunction(SampleRate sampleRate, double amplitude) {
        this.sampleRate = sampleRate;
        this.amplitude = amplitude;
    }

    public double getVolume(TimeInSeconds timeDifference) {
        if (timeDifference.getValue() < 0) {
            return 0;
        }

        return getVolume2(timeDifference);
    }

    protected abstract double getVolume2(TimeInSeconds timeDifference);

}