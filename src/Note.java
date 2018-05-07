public class Note {

    private double frequency;
    private long startingTick;

    public Note(double frequency, long startingTick){
        this.frequency = frequency;
        this.startingTick = startingTick;
    }

    public double getAmplitude(float sampleRate, long tick) {
        if(startingTick>tick){
            return (byte) 0;
        }
        long tickDifference = tick-getStartingTick();
        double volume = getVolume(tickDifference);
        double angle = tickDifference / ( sampleRate / getFrequency()) * 2.0 * Math.PI;
        return (Math.sin(angle) * volume);
    }

    private double getVolume(long tickDifference) {
        return 1000./(1000.+tickDifference);
    }

    public boolean isDead(long tick, double marginalSampleSize){
        return Math.abs(getVolume(tick-getStartingTick()))<marginalSampleSize;
    }

    public double getFrequency() {
        return frequency;
    }

    public long getStartingTick() {
        return startingTick;
    }

}
