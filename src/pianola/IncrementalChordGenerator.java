package pianola;

import frequency.Frequency;
import gui.SpectrumManager;
import gui.buckets.Buckets;
import gui.GUI;

public class IncrementalChordGenerator extends SimpleChordGenerator {
    protected int noteIndex = 0;

    public IncrementalChordGenerator(GUI gui, SpectrumManager spectrumManager, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener) {
        super(spectrumManager, chordSize, centerFrequency, totalMargin, hardLeftBorder, hardRightBorder, repetitionDampener, gui.spectrumWindow);
    }

    @Override
    protected void updateNotes(Buckets maximaBuckets, Integer[] leftBorders, Integer[] rightBorders) {
        frequencies[noteIndex] = updateNote(maximaBuckets, leftBorders[noteIndex], rightBorders[noteIndex]);

        noteIndex = (noteIndex + 1) % chordSize;
    }
}