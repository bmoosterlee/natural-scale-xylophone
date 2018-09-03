package notes;

import main.SampleRate;

public class PrecalculatedLinearEnvelope extends LinearEnvelope{
    Double[] volumes;

    public PrecalculatedLinearEnvelope(long startingSampleCount, SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        super(startingSampleCount, sampleRate, amplitude, lengthInSeconds);

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