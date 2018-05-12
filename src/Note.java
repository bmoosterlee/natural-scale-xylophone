public class Note {

    private double frequency;
    private long startingSampleCount;

    public Note(double frequency, long startingSampleCount){
        this.frequency = frequency;
        this.startingSampleCount = startingSampleCount;
    }

    public double getAmplitude(float sampleRate, long currentSampleCount) {
        if(startingSampleCount >currentSampleCount){
            return (byte) 0;
        }
        double volume = getVolume(currentSampleCount);
        long sampleDifference = currentSampleCount- getStartingSampleCount();
        double angle = sampleDifference / ( sampleRate / getFrequency()) * 2.0 * Math.PI;
        return (Math.sin(angle) * volume);
    }

    public double getVolume(long currentSampleCount) {
        return 1000./(1000.+(currentSampleCount- getStartingSampleCount()));
    }

    public boolean isDead(long currentSampleCount, double marginalSampleSize){
        return Math.abs(getVolume(currentSampleCount))<marginalSampleSize;
    }

    public double getFrequency() {
        return frequency;
    }

    public long getStartingSampleCount() {
        return startingSampleCount;
    }

}
