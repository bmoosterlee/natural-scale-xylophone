package pianola.patterns;

import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.buckets.Buckets;
import main.BoundedBuffer;
import main.InputPort;
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

    private final InputPort<Buckets> notesInput;
    private final BoundedBuffer<Buckets> notesBuffer;
    final BoundedBuffer<Buckets> harmonicsBuffer;

    Sweep(BoundedBuffer<Buckets> notesBuffer, BoundedBuffer<Buckets> harmonicsBuffer, int size, Frequency centerFrequency, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;
        this.size = size;

        notesInput = new InputPort<>(notesBuffer);

        this.notesBuffer = notesBuffer;
        this.harmonicsBuffer = harmonicsBuffer;

        sequencer = new Sequencer(size, 1);

        totalMargin = spectrumWindow.getX(spectrumWindow.getCenterFrequency().multiplyBy(1.5)) -
                spectrumWindow.getX(spectrumWindow.getCenterFrequency());

        simpleChordGenerator =
                getSimpleChordGenerator(centerFrequency);
        try {
            generateNewChord();
        }
        catch(NullPointerException ignored){

        }
    }

    SimpleChordGenerator getSimpleChordGenerator(Frequency centerFrequency) {
        return new SimpleChordGenerator(
                harmonicsBuffer,
                1,
                centerFrequency,
                totalMargin,
                spectrumWindow.getX(spectrumWindow.lowerBound),
                spectrumWindow.getX(spectrumWindow.upperBound.divideBy(2.0)), 3, spectrumWindow);
    }

    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies = new HashSet<>();

        updateNoteBuckets();

        try {
            if (sequencer.isResetting()) {
                generateNewChord();
                frequencies.addAll(new ChordPlayer(simpleChordGenerator.getFrequencies()).playPattern());
            }
            else {
                moveRight();
                frequencies.addAll(new ChordPlayer(sweepGenerator.getFrequencies()).playPattern());
            }
            sequencer.tick();
        } catch (NullPointerException e) {
            try {
                generateNewChord();
            } catch (NullPointerException ignored) {

            }
        }

        return frequencies;
    }

    private void updateNoteBuckets() {
        try {
            Buckets origNoteBuckets;
            try {
                origNoteBuckets = notesInput.consume();
            }
            catch(NullPointerException e){
                origNoteBuckets = new Buckets();
            }

            simpleChordGenerator.noteHistory = simpleChordGenerator.noteHistory.addNewBuckets(origNoteBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void generateNewChord() {
        simpleChordGenerator.generateChord();
        sweepGenerator = simpleChordGenerator;
    }

    void moveRight() {
        sweepGenerator = findNextSweepGenerator();
        sweepGenerator.noteHistory = simpleChordGenerator.noteHistory;
        sweepGenerator.generateChord();
    }

    SimpleChordGenerator findNextSweepGenerator() {
        Frequency previousFrequency = spectrumWindow.getFrequency(spectrumWindow.getX(sweepGenerator.getFrequencies()[0]) +
                simpleChordGenerator.margin + 1);
        return new SimpleChordGenerator(
                harmonicsBuffer,
                1,
                                  previousFrequency,
                                  totalMargin,
                spectrumWindow.getX(previousFrequency),
                spectrumWindow.getX(spectrumWindow.upperBound), 1, spectrumWindow);
    }
}
