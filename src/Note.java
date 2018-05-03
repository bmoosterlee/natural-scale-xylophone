public class Note {

    private double frequency;
    private long startingTick;

    public Note(double frequency, long startingTick){
        this.frequency = frequency;
        this.startingTick = startingTick;
    }

    public byte getAmplitude(float sampleRate, long tick) {
        long tickDifference = tick-getStartingTick();
        double volume = 100. * 1000./(1000.+tickDifference);
        double angle = tickDifference / ( sampleRate / getFrequency()) * 2.0 * Math.PI;
        return (byte) (Math.sin(angle) * volume);
    }

    public double getFrequency() {
        return frequency;
    }

    public long getStartingTick() {
        return startingTick;
    }

}
