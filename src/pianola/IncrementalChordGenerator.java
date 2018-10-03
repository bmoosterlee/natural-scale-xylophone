package pianola;

import frequency.Frequency;
import gui.Buckets;
import gui.GUI;

public class IncrementalChordGenerator extends SimpleChordGenerator {
    protected int noteIndex = 0;

    public IncrementalChordGenerator(GUI gui, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder) {
        super(gui, chordSize, centerFrequency, totalMargin, hardLeftBorder, gui.spectrumWindow.getX(gui.spectrumWindow.upperBound), 3);
    }

    @Override
    protected void updateNotes(Buckets maximaBuckets, Integer[] leftBorders, Integer[] rightBorders) {
        frequencies[noteIndex] = updateNote(maximaBuckets, leftBorders[noteIndex], rightBorders[noteIndex]);

        noteIndex = (noteIndex + 1) % chordSize;
    }
}