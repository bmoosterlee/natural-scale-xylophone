public class Note {

    private double frequency;
    private long startingTick;

    public Note(double frequency, long startingTick){
        this.frequency = frequency;
        this.startingTick = startingTick;
    }

    public byte getAmplitude(float sampleRate, long tick) {
        if(startingTick>tick){
            return (byte) 0;
        }
        long tickDifference = tick-getStartingTick();
        double volume = getVolume(tickDifference);
        double angle = tickDifference / ( sampleRate / getFrequency()) * 2.0 * Math.PI;
        return (byte) (Math.sin(angle) * volume);
    }

    private double getVolume(long tickDifference) {
        return 100. * 1000./(1000.+tickDifference);
    }
    public double getFrequency() {
        return frequency;
    }

    public long getStartingTick() {
        return startingTick;
    }

}
