package pianola;

import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;

class StaticGenerator extends SimpleChordGenerator {

    public StaticGenerator(BoundedBuffer<SpectrumState> buffer, Frequency centerFrequency, SpectrumWindow spectrumWindow) {
        super(buffer, 1, centerFrequency, 0, 0, 0, 3, spectrumWindow);
        frequencies = new Frequency[]{centerFrequency};
    }

    @Override
    public void generateChord(){

    }

}
