package pianola;

import frequency.Frequency;
import gui.GUI;
import gui.SpectrumManager;
import gui.SpectrumWindow;

public class StaticGenerator extends SimpleChordGenerator {

    public StaticGenerator(SpectrumManager spectrumManager, Frequency centerFrequency, SpectrumWindow spectrumWindow) {
        super(spectrumManager, 1, centerFrequency, 0, 0, 0, 3, spectrumWindow);
        frequencies = new Frequency[]{centerFrequency};
    }

    @Override
    public void generateChord(){

    }

}
