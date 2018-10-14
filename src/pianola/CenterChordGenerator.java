package pianola;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import main.BoundedBuffer;

class CenterChordGenerator extends IncrementalChordGenerator {
    private final int centerFrequency;

    public CenterChordGenerator(BoundedBuffer<Buckets> harmonicsBuffer, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener, SpectrumWindow spectrumWindow) {
        super(harmonicsBuffer, chordSize, centerFrequency, totalMargin, hardLeftBorder, hardRightBorder, repetitionDampener, spectrumWindow);
        this.centerFrequency = spectrumWindow.getX(centerFrequency);
    }


    @Override
    public int findCenterFrequency(){
        return centerFrequency;
    }

}