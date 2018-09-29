package pianola;

import frequency.Frequency;
import gui.Buckets;
import gui.GUI;

public class IncrementalChordGenerator extends SimpleChordGenerator {
    protected int noteIndex = 0;

    public IncrementalChordGenerator(GUI gui, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder) {
        super(gui, chordSize, centerFrequency, totalMargin, hardLeftBorder, gui.spectrumWindow.getX(gui.spectrumWindow.upperBound));
    }

    @Override
    protected void updateNotes(Buckets maximaBuckets, Buckets centerProximity, Integer[] leftBorders, Integer[] rightBorders) {
        updateNote(maximaBuckets, centerProximity, noteIndex, leftBorders[noteIndex], rightBorders[noteIndex]);

        noteIndex = (noteIndex + 1) % chordSize;
    }
}