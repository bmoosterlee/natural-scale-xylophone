package pianola;

import frequency.Frequency;
import gui.GUI;
import gui.SpectrumManager;

public class StaticGenerator extends SimpleChordGenerator {
    GUI gui;

    public StaticGenerator(GUI gui, SpectrumManager spectrumManager, Frequency centerFrequency) {
        super(gui, spectrumManager, 1, centerFrequency, 0, 0, 0, 3);
        frequencies = new Frequency[]{centerFrequency};
    }

    @Override
    public void generateChord(){

    }

}
