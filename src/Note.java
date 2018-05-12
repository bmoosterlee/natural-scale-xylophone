public class Note {

    private double frequency;
    private long startingSampleCount;

    public Note(double frequency, long startingSampleCount){
        this.frequency = frequency;
        this.startingSampleCount = startingSampleCount;
    }

    public double getAmplitude(float sampleRate, long sampleCount) {
        if(startingSampleCount >sampleCount){
            return (byte) 0;
        }
        double volume = getVolume(sampleCount);
        long tickDifference = sampleCount- getStartingSampleCount();
        double angle = tickDifference / ( sampleRate / getFrequency()) * 2.0 * Math.PI;
        return (Math.sin(angle) * volume);
    }

    public double getVolume(long sampleCount) {
        return 1000./(1000.+(sampleCount- getStartingSampleCount()));
    }

    public boolean isDead(long sampleCount, double marginalSampleSize){
        return Math.abs(getVolume(sampleCount))<marginalSampleSize;
    }

    public double getFrequency() {
        return frequency;
    }

    public long getStartingSampleCount() {
        return startingSampleCount;
    }

}
