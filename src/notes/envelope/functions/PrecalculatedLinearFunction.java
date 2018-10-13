package notes.envelope.functions;

import sound.SampleRate;
import time.TimeInSeconds;

public class PrecalculatedLinearFunction extends LinearFunction {
    private final Double[] volumes;

    public PrecalculatedLinearFunction(SampleRate sampleRate, double amplitude, TimeInSeconds lengthInSeconds) {
        super(sampleRate, amplitude, lengthInSeconds);

        long totalSamples = sampleRate.asSampleCount(lengthInSeconds);
        volumes = new Double[(int) totalSamples];
        for(int i = 0; i<totalSamples; i++){
            volumes[i] = super.getVolume(sampleRate.asTime(i));
        }
    }

    @Override
    public double getVolume(TimeInSeconds timeDifference) {
        try {
            return volumes[(int) sampleRate.asSampleCount(timeDifference)];
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0.;
        }
    }
}