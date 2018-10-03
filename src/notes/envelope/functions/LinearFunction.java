package notes.envelope.functions;

import sound.SampleRate;
import time.TimeInSeconds;

public class LinearFunction extends DeterministicFunction {
    
    public LinearFunction(SampleRate sampleRate, double amplitude, TimeInSeconds totalTime) {
        super(sampleRate, amplitude, totalTime);
    }

    @Override
    protected double getVolume2(TimeInSeconds timeDifference) {
        if (timeDifference.getValue() >= totalTime.getValue()) {
            return 0.;
        } else {
            double attack = 0.1;
            if (timeDifference.getValue() < totalTime.getValue() * attack) {
                return amplitude * timeDifference.getValue() / (totalTime.getValue() * attack);
            } else {
                return amplitude * (1 - (timeDifference.getValue() - totalTime.getValue() * attack) / (totalTime.getValue() * (1 - attack)));
            }
        }
    }

}