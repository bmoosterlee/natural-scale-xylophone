public class Note {

    private double frequency;
    private long startingSampleCount;

    public Note(double frequency, long startingSampleCount){
        this.frequency = frequency;
        this.startingSampleCount = startingSampleCount;
    }

    public double getAmplitude(float sampleRate, long sampleCount) {
        if(!hasStarted(sampleCount)){
            return (byte) 0;
        }
        double volume = getVolume(sampleCount);
        long sampleCountDifference = sampleCount - getStartingSampleCount();
        double timeDifference = sampleCountDifference / sampleRate;
        double angle = timeDifference * getFrequency() * 2.0 * Math.PI;
        return (Math.sin(angle) * volume);
    }

    public boolean hasStarted(long sampleCount) {
        return startingSampleCount <sampleCount;
    }

    public double getVolume(long sampleCount) {
        return 1000./(1000.+(sampleCount- getStartingSampleCount()));
    }

    public double getFrequency() {
        return frequency;
    }

    public long getStartingSampleCount() {
        return startingSampleCount;
    }

}
