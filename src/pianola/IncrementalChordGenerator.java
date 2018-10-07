package pianola;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.GUI;

public class IncrementalChordGenerator extends SimpleChordGenerator {
    protected int noteIndex = 0;

    public IncrementalChordGenerator(GUI gui, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener) {
        super(gui, chordSize, centerFrequency, totalMargin, hardLeftBorder, hardRightBorder, repetitionDampener);
    }

    @Override
    protected void updateNotes(Buckets maximaBuckets, Integer[] leftBorders, Integer[] rightBorders) {
        frequencies[noteIndex] = updateNote(maximaBuckets, leftBorders[noteIndex], rightBorders[noteIndex]);

        noteIndex = (noteIndex + 1) % chordSize;
    }
}