package pianola;

import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.buckets.Buckets;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;

class IncrementalChordGenerator extends SimpleChordGenerator {
    int noteIndex = 0;

    public IncrementalChordGenerator(BoundedBuffer<SpectrumState> buffer, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener, SpectrumWindow spectrumWindow) {
        super(buffer, chordSize, centerFrequency, totalMargin, hardLeftBorder, hardRightBorder, repetitionDampener, spectrumWindow);
    }

    @Override
    protected void updateNotes(Buckets maximaBuckets, Integer[] leftBorders, Integer[] rightBorders) {
        frequencies[noteIndex] = updateNote(maximaBuckets, leftBorders[noteIndex], rightBorders[noteIndex]);

        noteIndex = (noteIndex + 1) % chordSize;
    }
}