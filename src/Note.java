public class Note {

    private double frequency;
    private long startingSampleCount;
    private final double noteLengthInSeconds = 3.;

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

    public double getVolume(float sampleRate, long sampleCount) {
        //TODO Create method with decay as an argument, let harmonics extract the volume for a longer decay, such that they remain on the screen longer
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

    public double getFrequency() {
        return frequency;
    }

    public long getStartingSampleCount() {
        return startingSampleCount;
    }

}
