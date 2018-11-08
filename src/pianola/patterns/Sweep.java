package pianola.patterns;

import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.buckets.Buckets;
import pianola.Sequencer;
import pianola.chordgen.SimpleChordGenerator;

import java.util.HashSet;
import java.util.Set;

public class Sweep implements PianolaPattern {
    //todo build a sweep which finds the octave higher, cuts the space from the tonic to the octave in -size- pieces
    //todo and lets each sweep find the highest value harmonic in that range.
    final int totalMargin;
    final int size;
    SimpleChordGenerator simpleChordGenerator;
    private SimpleChordGenerator sweepGenerator;

    final Sequencer sequencer;
    final SpectrumWindow spectrumWindow;

    protected int inaudibleFrequencyMargin;

    Sweep(int size, Frequency centerFrequency, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin) {
        this.spectrumWindow = spectrumWindow;
        this.size = size;

        this.inaudibleFrequencyMargin = inaudibleFrequencyMargin;

        sequencer = new Sequencer(size, 1);

        totalMargin = spectrumWindow.getX(spectrumWindow.getCenterFrequency().multiplyBy(1.5)) -
                spectrumWindow.getX(spectrumWindow.getCenterFrequency());

        simpleChordGenerator =
                getSimpleChordGenerator(centerFrequency);
    }

    SimpleChordGenerator getSimpleChordGenerator(Frequency centerFrequency) {
        return new SimpleChordGenerator(
                1,
                centerFrequency,
                totalMargin,
                spectrumWindow.getX(spectrumWindow.lowerBound),
                spectrumWindow.getX(spectrumWindow.upperBound.divideBy(2.0)),
                spectrumWindow
        );
    }

    public Set<Frequency> playPattern(Buckets noteBuckets, Buckets harmonicsBuckets) {
        Set<Frequency> frequencies = new HashSet<>();

        try {
            if (sequencer.isResetting()) {
                generateNewChord(noteBuckets, harmonicsBuckets);
                frequencies.addAll(new ChordPlayer(simpleChordGenerator.getFrequencies()).playPattern(noteBuckets, harmonicsBuckets));
            }
            else {
                moveRight(noteBuckets, harmonicsBuckets);
                frequencies.addAll(new ChordPlayer(sweepGenerator.getFrequencies()).playPattern(noteBuckets, harmonicsBuckets));
            }
            sequencer.tick();
        } catch (NullPointerException e) {
            try {
                generateNewChord(noteBuckets, harmonicsBuckets);
            } catch (NullPointerException ignored) {

            }
        }

        return frequencies;
    }

    void generateNewChord(Buckets noteBuckets, Buckets harmonicsBuckets) {
        simpleChordGenerator.generateChord(noteBuckets, harmonicsBuckets);
        sweepGenerator = simpleChordGenerator;
    }

    void moveRight(Buckets noteBuckets, Buckets harmonicsBuckets) {
        sweepGenerator = findNextSweepGenerator();
        sweepGenerator.generateChord(noteBuckets, harmonicsBuckets);
    }

    SimpleChordGenerator findNextSweepGenerator() {
        Frequency previousFrequency = spectrumWindow.getFrequency(spectrumWindow.getX(sweepGenerator.getFrequencies()[0]) +
                simpleChordGenerator.margin + 1);
        return new SimpleChordGenerator(
                1,
                                  previousFrequency,
                                  totalMargin,
                spectrumWindow.getX(previousFrequency),
                spectrumWindow.getX(spectrumWindow.upperBound),
                spectrumWindow
        );
    }
}
