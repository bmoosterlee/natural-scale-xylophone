package notes;

import main.SampleRate;

public class LinearEnvelope extends Envelope{
    double lengthInSeconds;
    
    public LinearEnvelope(long startingSampleCount, SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        super(startingSampleCount, sampleRate, amplitude);
        this.lengthInSeconds = lengthInSeconds;
    }

    @Override
    public double getVolume(double timeDifference) {
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