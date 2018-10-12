package pianola;

import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumManager;
import gui.buckets.Buckets;

public class BottomIncrementalChordGenerator extends IncrementalChordGenerator {

    public BottomIncrementalChordGenerator(SpectrumManager spectrumManager, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener, SpectrumWindow spectrumWindow) {
        super(spectrumManager, chordSize, centerFrequency, totalMargin, hardLeftBorder, hardRightBorder, repetitionDampener, spectrumWindow);
        noteIndex = 1;
    }

    @Override
    protected void updateNotes(Buckets maximaBuckets, Integer[] leftBorders, Integer[] rightBorders) {
        frequencies[0] = updateNote(maximaBuckets, leftBorders[0], rightBorders[0]);

        frequencies[noteIndex] = updateNote(maximaBuckets, leftBorders[noteIndex], rightBorders[noteIndex]);

        noteIndex = 1 + (noteIndex + 1) % chordSize;
    }
}