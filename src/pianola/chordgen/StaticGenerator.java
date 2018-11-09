package pianola.chordgen;

import frequency.Frequency;
import spectrum.buckets.Buckets;
import spectrum.SpectrumWindow;

public class StaticGenerator extends SimpleChordGenerator {

    public StaticGenerator(Frequency centerFrequency, SpectrumWindow spectrumWindow) {
        super(1, centerFrequency, 0, 0, 0, spectrumWindow);
        frequencies = new Frequency[]{centerFrequency};
    }

    @Override
    public void generateChord(Buckets noteBuckets, Buckets harmonicsBuckets){

    }

}
