package pianola.chordgen;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import main.BoundedBuffer;

public class StaticGenerator extends SimpleChordGenerator {

    public StaticGenerator(BoundedBuffer<Buckets> harmonicsBuffer, Frequency centerFrequency, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin) {
        super(harmonicsBuffer, 1, centerFrequency, 0, 0, 0, 3, spectrumWindow, inaudibleFrequencyMargin);
        frequencies = new Frequency[]{centerFrequency};
    }

    @Override
    public void generateChord(){

    }

}
