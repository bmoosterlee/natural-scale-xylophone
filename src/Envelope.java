public class Envelope {
    private long startingSampleCount;
    private float sampleRate;

    public Envelope(long startingSampleCount, float sampleRate) {
        this.startingSampleCount = startingSampleCount;
        this.sampleRate = sampleRate;
    }

    public double getVolume(long sampleCount) {
        return getVolumeAsymptotic(sampleCount, 0.25, 1.5);
    }

    double getVolumeLinear(long sampleCount, double noteLengthInSeconds) {
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        double timeDifference = sampleCountDifference / sampleRate;

        if (timeDifference < 0) {
            return 0;
        }
        if (timeDifference >= noteLengthInSeconds) {
            return 0;
        } else {
            return 1.0 - timeDifference / noteLengthInSeconds;
        }
    }

    double getVolumeAsymptotic(long sampleCount, double amplitude, double exponent) {
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        double timeDifference = sampleCountDifference / sampleRate;

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
        return sampleCountDifference / sampleRate;
    }
}