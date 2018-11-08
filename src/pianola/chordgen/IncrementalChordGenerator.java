package pianola.chordgen;

import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.buckets.Buckets;

public class IncrementalChordGenerator extends SimpleChordGenerator {
    private int noteIndex = 0;

    public IncrementalChordGenerator(int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, SpectrumWindow spectrumWindow) {
        super(chordSize, centerFrequency, totalMargin, hardLeftBorder, hardRightBorder, spectrumWindow);
    }

    @Override
    protected void updateNotes(Buckets maximaBuckets, Integer[] leftBorders, Integer[] rightBorders) {
        frequencies[noteIndex] = updateNote(maximaBuckets, leftBorders[noteIndex], rightBorders[noteIndex]);

        noteIndex = (noteIndex + 1) % chordSize;
    }
}