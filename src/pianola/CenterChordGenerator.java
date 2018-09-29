package pianola;

import frequency.Frequency;
import gui.GUI;

public class CenterChordGenerator extends IncrementalChordGenerator {
    int centerFrequency;

    public CenterChordGenerator(GUI gui, int chordSize, Frequency centerFrequency, int totalMargin, int hardLeftBorder) {
        super(gui, chordSize, centerFrequency, totalMargin, hardLeftBorder);
        this.centerFrequency = gui.spectrumWindow.getX(centerFrequency);
    }

    @Override
    public int findCenterFrequency(){
        return centerFrequency;
    }

}