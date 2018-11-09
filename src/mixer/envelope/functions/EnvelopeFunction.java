package mixer.envelope.functions;

import sound.SampleRate;
import time.TimeInSeconds;

public abstract class EnvelopeFunction {
    final SampleRate sampleRate;
    final double amplitude;

    EnvelopeFunction(SampleRate sampleRate, double amplitude) {
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