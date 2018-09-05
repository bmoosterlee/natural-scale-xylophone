package notes;

import main.SampleRate;

public class LinearEnvelopeFunction extends EnvelopeFunction{
    double lengthInSeconds;
    
    public LinearEnvelopeFunction(SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        super(sampleRate, amplitude);
        this.lengthInSeconds = lengthInSeconds;
    }

    @Override
    protected double getVolume2(double timeDifference) {
        if (timeDifference >= lengthInSeconds) {
            return 0.;
        } else {
            double attack = 0.1;
            if (timeDifference < lengthInSeconds * attack) {
                return amplitude * timeDifference / (lengthInSeconds * attack);
            } else {
                return amplitude * (1 - (timeDifference - lengthInSeconds * attack) / (lengthInSeconds * (1 - attack)));
            }
        }
    }
}