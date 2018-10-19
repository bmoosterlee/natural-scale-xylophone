package pianola;

public class Sequencer {
    public final int notesPerMeasure;
    private final int measuresTillReset;
    public int i = 0;
    public int j = 0;

    public Sequencer(int notesPerMeasure, int measuresTillReset) {
        this.notesPerMeasure = notesPerMeasure;
        this.measuresTillReset = measuresTillReset;
    }

    public void tick() {
        i = (i + 1) % notesPerMeasure;
        if (i == 0) {
            j = (j + 1) % measuresTillReset;
        }
    }

    public boolean isResetting() {
        return i==0 && j==0;
    }
}