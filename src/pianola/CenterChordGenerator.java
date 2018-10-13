package pianola;

import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;

class CenterChordGenerator extends IncrementalChordGenerator {
    private final int centerFrequency;

    public CenterChordGenerator(BoundedBuffer<SpectrumState> buffer, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener, SpectrumWindow spectrumWindow) {
        super(buffer, chordSize, centerFrequency, totalMargin, hardLeftBorder, hardRightBorder, repetitionDampener, spectrumWindow);
        this.centerFrequency = spectrumWindow.getX(centerFrequency);
    }


    @Override
    public int findCenterFrequency(){
        return centerFrequency;
    }

}