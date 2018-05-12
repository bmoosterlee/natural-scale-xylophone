public class Note {

    private double frequency;
    private long startingTick;

    public Note(double frequency, long startingTick){
        this.frequency = frequency;
        this.startingTick = startingTick;
    }

    public double getAmplitude(float sampleRate, long currentTick) {
        if(startingTick>currentTick){
            return (byte) 0;
        }
        double volume = getVolume(currentTick);
        long tickDifference = currentTick-getStartingTick();
        double angle = tickDifference / ( sampleRate / getFrequency()) * 2.0 * Math.PI;
        return (Math.sin(angle) * volume);
    }

    public double getVolume(long currentTick) {
        return 1000./(1000.+(currentTick-getStartingTick()));
    }

    public boolean isDead(long currentTick, double marginalSampleSize){
        return Math.abs(getVolume(currentTick))<marginalSampleSize;
    }

    public double getFrequency() {
        return frequency;
    }

    public long getStartingTick() {
        return startingTick;
    }

}
