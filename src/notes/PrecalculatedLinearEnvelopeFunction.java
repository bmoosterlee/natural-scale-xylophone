package notes;

import main.SampleRate;

public class PrecalculatedLinearEnvelopeFunction extends LinearEnvelopeFunction {
    Double[] volumes;

    public PrecalculatedLinearEnvelopeFunction(SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        super(sampleRate, amplitude, lengthInSeconds);

        int totalSamples = (int) (sampleRate.sampleRate * lengthInSeconds);
        volumes = new Double[totalSamples];
        for(int i = 0; i<totalSamples; i++){
            volumes[i] = super.getVolume(sampleRate.asTime(i));
        }
    }

    @Override
    public double getVolume(double timeDifference) {
        try {
            return volumes[(int) sampleRate.asSampleCount(timeDifference)];
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0.;
        }
    }
}