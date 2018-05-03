public class Note {

    private double frequency;
    private long startingTick;

    public Note(double frequency, long startingTick){
        this.frequency = frequency;
        this.startingTick = startingTick;
    }

    public double getFrequency() {
        return frequency;
    }

    public long getStartingTick() {
        return startingTick;
    }

}
