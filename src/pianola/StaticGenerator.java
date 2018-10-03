package pianola;

import frequency.Frequency;
import gui.GUI;

public class StaticGenerator extends SimpleChordGenerator {
    GUI gui;

    public StaticGenerator(GUI gui, Frequency centerFrequency) {
        super(gui, 1, centerFrequency, 0, 0, 0, 3);
        frequencies = new Frequency[]{centerFrequency};
    }

    @Override
    public void generateChord(){

    }

}
