package notes.envelope.functions;

import main.SampleRate;

public class LinearFunction extends DeterministicFunction {
    
    public LinearFunction(SampleRate sampleRate, double amplitude, double totalTime) {
        super(sampleRate, amplitude, totalTime);
    }

    @Override
    protected double getVolume2(double timeDifference) {
        if (timeDifference >= totalTime) {
            return 0.;
        } else {
            double attack = 0.1;
            if (timeDifference < totalTime * attack) {
                return amplitude * timeDifference / (totalTime * attack);
            } else {
                return amplitude * (1 - (timeDifference - totalTime * attack) / (totalTime * (1 - attack)));
            }
        }
    }

    //todo implement seconds data type, implement samplecount datatype, implement miliseconds datatype, implement nanosecs
}