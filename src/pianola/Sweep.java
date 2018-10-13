package pianola;

import frequency.Frequency;
import gui.spectrum.state.SpectrumManager;
import gui.spectrum.SpectrumWindow;
import gui.buckets.Buckets;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;
import main.InputPort;

import java.util.HashSet;
import java.util.Set;

public class Sweep implements PianolaPattern {
    //todo build a sweep which finds the octave higher, cuts the space from the tonic to the octave in -size- pieces
    //todo and lets each sweep find the highest value harmonic in that range.
    final int totalMargin;
    int size;
    SimpleChordGenerator simpleChordGenerator;
    SimpleChordGenerator sweepGenerator;

    protected Sequencer sequencer;
    final SpectrumWindow spectrumWindow;

    InputPort<SpectrumState> spectrumStateInput;
    BoundedBuffer buffer;

    public Sweep(BoundedBuffer<SpectrumState> buffer, int size, Frequency centerFrequency, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;
        this.size = size;

        spectrumStateInput = new InputPort<>(buffer);
        this.buffer = buffer;

        sequencer = new Sequencer(size, 1);

        totalMargin = spectrumWindow.getX(spectrumWindow.getCenterFrequency().multiplyBy(1.5)) -
                spectrumWindow.getX(spectrumWindow.getCenterFrequency());

        simpleChordGenerator =
                getSimpleChordGenerator(centerFrequency);
        try {
            generateNewChord();
        }
        catch(NullPointerException e){

        }
    }

    protected SimpleChordGenerator getSimpleChordGenerator(Frequency centerFrequency) {
        return new SimpleChordGenerator(
                buffer,
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
            } catch (NullPointerException e2) {

            }
        }

        return frequencies;
    }

    private void updateNoteBuckets() {
        try {
            SpectrumState spectrumState = spectrumStateInput.consume();

            Buckets origNoteBuckets;
            try {
                origNoteBuckets = spectrumState.noteBuckets;
            }
            catch(NullPointerException e){
                origNoteBuckets = new Buckets();
            }

            simpleChordGenerator.noteHistory = simpleChordGenerator.noteHistory.addNewBuckets(origNoteBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void generateNewChord() {
        simpleChordGenerator.generateChord();
        sweepGenerator = simpleChordGenerator;
    }

    protected void moveRight() {
        sweepGenerator = findNextSweepGenerator();
        sweepGenerator.noteHistory = simpleChordGenerator.noteHistory;
        sweepGenerator.generateChord();
    }

    protected SimpleChordGenerator findNextSweepGenerator() {
        Frequency previousFrequency = spectrumWindow.getFrequency(spectrumWindow.getX(sweepGenerator.getFrequencies()[0]) +
                simpleChordGenerator.margin + 1);
        return new SimpleChordGenerator(
                buffer,
                1,
                                  previousFrequency,
                                  totalMargin,
                spectrumWindow.getX(previousFrequency),
                spectrumWindow.getX(spectrumWindow.upperBound), 1, spectrumWindow);
    }
}
