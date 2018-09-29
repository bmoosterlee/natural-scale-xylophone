package pianola;

import frequency.Frequency;
import gui.Buckets;
import gui.GUI;

public class BottomIncrementalChordGenerator extends IncrementalChordGenerator {

    public BottomIncrementalChordGenerator(GUI gui, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder) {
        super(gui, chordSize, centerFrequency, totalMargin, hardLeftBorder);
        noteIndex = 1;
    }

    @Override
    protected void updateNotes(Buckets maximaBuckets, Buckets centerProximity, Integer[] leftBorders, Integer[] rightBorders) {
        updateNote(maximaBuckets, centerProximity, 0, leftBorders[0], rightBorders[0]);

        updateNote(maximaBuckets, centerProximity, noteIndex, leftBorders[noteIndex], rightBorders[noteIndex]);

        noteIndex = 1 + (noteIndex + 1) % chordSize;
    }
}