public class Note {

    private double frequency;
    private long startingSampleCount;

    public Note(double frequency, long startingSampleCount){
        this.frequency = frequency;
        this.startingSampleCount = startingSampleCount;
    }

    public double getAmplitude(float sampleRate, long sampleCount) {
        double volume = getVolume(sampleRate, sampleCount);
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        double timeDifference = sampleCountDifference / sampleRate;
        double angle = timeDifference * getFrequency() * 2.0 * Math.PI;
        return (Math.sin(angle) * volume);
    }

    public double getVolume(float sampleRate, long sampleCount){
        return getVolumeAsymptotic(sampleRate, sampleCount, 0.8, 1.5);
    }

    public double getVolumeLinear(float sampleRate, long sampleCount, double noteLengthInSeconds) {
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        double timeDifference = sampleCountDifference / sampleRate;

        if(timeDifference<0){
            return 0;
        }
        if(timeDifference>=noteLengthInSeconds){
            return 0;
        }
        else {
            return 1.0 - timeDifference / noteLengthInSeconds;
        }
    }

    public double getVolumeAsymptotic(float sampleRate, long sampleCount, double amplitude, double exponent) {
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        double timeDifference = sampleCountDifference / sampleRate;

        if(timeDifference<0){
            return 0;
        }
        else {
            return amplitude/(Math.pow(timeDifference, exponent)+1);
        }
    }

    public double getFrequency() {
        return frequency;
    }

    public long getStartingSampleCount() {
        return startingSampleCount;
    }

}
