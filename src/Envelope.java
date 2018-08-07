public class Envelope {
    private final long startingSampleCount;
    private final float sampleRate;
    protected double amplitude = 0.05;
    private double startingTime;

    public Envelope(long startingSampleCount, float sampleRate) {
        this.startingSampleCount = startingSampleCount;
        this.sampleRate = sampleRate;
        startingTime = getTimeAsDouble(getStartingSampleCount());
    }

    public double getVolume(long sampleCount) {
        // return getVolumeAsymptotic(sampleCount, amplitude, 1.5);
        return getVolumeLinear(sampleCount, amplitude, 50.0);
    }

    double getVolumeLinear(long sampleCount, double amplitude, double noteLengthInSeconds) {
        double timeDifference = getTimeDifference(sampleCount);

        if (timeDifference < 0) {
            return 0;
        }
        if (timeDifference >= noteLengthInSeconds) {
            return 0;
        } else {
            return amplitude - timeDifference / noteLengthInSeconds;
        }
    }

    double getVolumeAsymptotic(long sampleCount, double amplitude, double exponent) {
        double timeDifference = getTimeDifference(sampleCount);

        if (timeDifference < 0) {
            return 0;
        } else {
            return amplitude / (Math.pow(timeDifference, exponent) + 1);
        }
    }

    public long getStartingSampleCount() {
        return startingSampleCount;
    }

    double getTimeDifference(long sampleCount) {
        return getTimeAsDouble(sampleCount) - startingTime;
    }

    double getTimeAsDouble(long sampleCount) {
        return sampleCount / getSampleRate();
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public double getStartingTime() {
        return startingTime;
    }

    public void setStartingTime(float startingTime) {
        this.startingTime = startingTime;
    }
}