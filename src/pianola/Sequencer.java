package pianola;

class Sequencer {
    final int notesPerMeasure;
    private final int measuresTillReset;
    int i = 0;
    int j = 0;

    public Sequencer(int notesPerMeasure, int measuresTillReset) {
        this.notesPerMeasure = notesPerMeasure;
        this.measuresTillReset = measuresTillReset;
    }

    void tick() {
        i = (i + 1) % notesPerMeasure;
        if (i == 0) {
            j = (j + 1) % measuresTillReset;
        }
    }

    boolean isResetting() {
        return i==0 && j==0;
    }
}