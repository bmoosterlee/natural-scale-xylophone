public class Envelope {
    private final long startingSampleCount;
    private final float sampleRate;
    protected double amplitude = 0.05;

    public Envelope(long startingSampleCount, float sampleRate) {
        this.startingSampleCount = startingSampleCount;
        this.sampleRate = sampleRate;
    }

    public double getVolume(long sampleCount) {
        // return getVolumeAsymptotic(sampleCount, amplitude, 1.5);
        return getVolumeLinear(sampleCount, amplitude, 50.0);
    }

    double getVolumeLinear(long sampleCount, double amplitude, double noteLengthInSeconds) {
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        double timeDifference = sampleCountDifference / getSampleRate();

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
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        double timeDifference = sampleCountDifference / getSampleRate();

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
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        return sampleCountDifference / getSampleRate();
    }

    public float getSampleRate() {
        return sampleRate;
    }
}