package pianola;

import frequency.Frequency;
import gui.GUI;
import gui.SpectrumManager;

public class CenterChordGenerator extends IncrementalChordGenerator {
    int centerFrequency;

    public CenterChordGenerator(GUI gui, SpectrumManager spectrumManager, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder, int hardRightBorder, int repetitionDampener) {
        super(gui, spectrumManager, chordSize, centerFrequency, totalMargin, hardLeftBorder, hardRightBorder, repetitionDampener);
        this.centerFrequency = gui.spectrumWindow.getX(centerFrequency);
    }


    @Override
    public int findCenterFrequency(){
        return centerFrequency;
    }

}