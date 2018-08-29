public class Envelope {
    private final long startingSampleCount;
    private final SampleRate sampleRate;
    protected double amplitude = 0.05;
    private double startingTime;

    public Envelope(long startingSampleCount, SampleRate sampleRate) {
        this.startingSampleCount = startingSampleCount;
        this.sampleRate = sampleRate;
        startingTime = sampleRate.asTime(getStartingSampleCount());
    }

    public double getVolume(long sampleCount) {
        // return getVolumeAsymptotic(sampleCount, amplitude, 10);
        return getVolumeLinear(sampleCount, amplitude, 1.0);
    }

    double getVolumeLinear(long sampleCount, double amplitude, double noteLengthInSeconds) {
        double timeDifference = getTimeDifference(sampleCount);

        if (timeDifference < 0) {
            return 0;
        }
        if (timeDifference >= noteLengthInSeconds) {
            return 0;
        }
        else {
            double attack = 0.1;
            if(timeDifference < noteLengthInSeconds*attack){
                return amplitude * timeDifference / (noteLengthInSeconds*attack);
            }
            else {
                return amplitude * (1 - (timeDifference-noteLengthInSeconds*attack) / (noteLengthInSeconds*(1-attack)));
            }
        }
    }

    double getVolumeAsymptotic(long sampleCount, double amplitude, double multiplier) {
        double timeDifference = getTimeDifference(sampleCount);

        if (timeDifference < 0) {
            return 0;
        } else {
            return amplitude / (timeDifference*multiplier + 1);
        }
    }

    public long getStartingSampleCount() {
        return startingSampleCount;
    }

    double getTimeDifference(long sampleCount) {
        return sampleRate.asTime(sampleCount) - startingTime;
    }

}