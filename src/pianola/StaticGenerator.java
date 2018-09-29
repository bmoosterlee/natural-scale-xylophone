package pianola;

import frequency.Frequency;
import gui.GUI;

public class StaticGenerator extends SimpleChordGenerator {
    GUI gui;

    public StaticGenerator(GUI gui, Frequency centerFrequency) {
        super(gui, 1, centerFrequency, 0, 0, 0);
        frequencies = new Integer[]{gui.spectrumWindow.getX(centerFrequency)};
    }

    @Override
    public void generateChord(){

    }

}
