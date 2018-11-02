package pianola.patterns;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import pianola.chordgen.IncrementalChordGenerator;
import pianola.chordgen.SimpleChordGenerator;

import java.util.HashSet;
import java.util.Set;

public class SimpleArpeggio implements PianolaPattern {
    private final int chordSize;
    private final SimpleChordGenerator simpleChordGenerator;

    private ArpeggiateUp arpeggiateUp;

    public SimpleArpeggio(int chordSize, SpectrumWindow spectrumWindow) {
        this.chordSize = chordSize;

        Frequency centerFrequency = spectrumWindow.getCenterFrequency();
        simpleChordGenerator = new IncrementalChordGenerator(
                chordSize,
                centerFrequency.divideBy(2.0),
                     spectrumWindow.getX(centerFrequency.multiplyBy(1.5)) -
                                    spectrumWindow.getX(centerFrequency),
                                spectrumWindow.getX(centerFrequency.divideBy(4.0)),
                                spectrumWindow.getX(centerFrequency.multiplyBy(4.0)), spectrumWindow);
    }

    public Set<Frequency> playPattern(Buckets noteBuckets, Buckets harmonicsBuckets) {
        Set<Frequency> frequencies = new HashSet<>();

        try {
            if (arpeggiateUp.sequencer.j == 0 && arpeggiateUp.sequencer.i == 0) {
                generateNewChord(noteBuckets, harmonicsBuckets);
            }
        }
        catch(NullPointerException e){
            try {
                generateNewChord(noteBuckets, harmonicsBuckets);
            }
            catch(NullPointerException ignored){

            }
        }

        try {
            frequencies.addAll(arpeggiateUp.playPattern(noteBuckets, harmonicsBuckets));
        }
        catch(NullPointerException ignored){

        }

        return frequencies;
    }

    private void generateNewChord(Buckets noteBuckets, Buckets harmonicsBuckets) {
        simpleChordGenerator.generateChord(noteBuckets, harmonicsBuckets);
        Frequency[] newFrequencies = simpleChordGenerator.getFrequencies();
        arpeggiateUp = new ArpeggiateUp(chordSize, 4, newFrequencies);
    }

}