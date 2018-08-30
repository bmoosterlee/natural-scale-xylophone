package notes;

import main.SampleRate;

public class Envelope {
    private final SampleRate sampleRate;
    protected double amplitude = 0.05;
    private long startingSampleCount;

    public Envelope(long startingSampleCount, SampleRate sampleRate) {
        this.sampleRate = sampleRate;
        this.startingSampleCount = startingSampleCount;
    }

    public double getVolume(long sampleCount) {
        // return getVolumeAsymptotic(getTimeDifference(sampleCount), amplitude, 10);
        double timeDifference = getTimeDifference(sampleCount);
        if (timeDifference < 0) {
            return 0;
        }
        return getVolumeLinear(amplitude, 1.0, timeDifference);
    }

    double getVolumeLinear(double amplitude, double noteLengthInSeconds, double timeDifference) {
        if (timeDifference >= noteLengthInSeconds) {
            return 0;
        }
        else {
            double attack = 0.1;
            if(timeDifference < noteLengthInSeconds*attack){
                return amplitude * timeDifference / (noteLengthInSeconds*attack);
            }
            else {
                return amplitude * (1 - (timeDifference -noteLengthInSeconds*attack) / (noteLengthInSeconds*(1-attack)));
            }
        }
    }

    double getVolumeAsymptotic(double amplitude, double multiplier, double timeDifference) {
        return amplitude / (timeDifference *multiplier + 1);
    }

    double getTimeDifference(long sampleCount) {
        return sampleRate.asTime(sampleCount - startingSampleCount);
    }

}