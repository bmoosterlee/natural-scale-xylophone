package pianola;

import frequency.Frequency;
import gui.SpectrumManager;
import gui.buckets.Buckets;
import gui.GUI;
import gui.SpectrumState;

import java.util.HashSet;
import java.util.Set;

public class Sweep implements PianolaPattern {
    //todo build a sweep which finds the octave higher, cuts the space from the tonic to the octave in -size- pieces
    //todo and lets each sweep find the highest value harmonic in that range.
    final int totalMargin;
    GUI gui;
    SpectrumManager spectrumManager;
    int size;
    SimpleChordGenerator simpleChordGenerator;
    SimpleChordGenerator sweepGenerator;

    protected Sequencer sequencer;

    public Sweep(Pianola pianola, SpectrumManager spectrumManager, int size, Frequency centerFrequency) {
        gui = pianola.getGui();
        this.spectrumManager = spectrumManager;
        this.size = size;
        sequencer = new Sequencer(size, 1);

        totalMargin = gui.spectrumWindow.getX(gui.spectrumWindow.getCenterFrequency().multiplyBy(1.5)) -
                gui.spectrumWindow.getX(gui.spectrumWindow.getCenterFrequency());

        simpleChordGenerator =
                getSimpleChordGenerator(centerFrequency);
        try {
            generateNewChord();
        }
        catch(NullPointerException e){

        }
    }

    protected SimpleChordGenerator getSimpleChordGenerator(Frequency centerFrequency) {
        return new SimpleChordGenerator(gui,
                spectrumManager,
                1,
                centerFrequency,
                totalMargin,
                gui.spectrumWindow.getX(gui.spectrumWindow.lowerBound),
                gui.spectrumWindow.getX(gui.spectrumWindow.upperBound.divideBy(2.0)), 3);
    }

    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies = new HashSet<>();

        updateNoteBuckets();

        try {
            if (sequencer.isResetting()) {
                generateNewChord();
                frequencies.addAll(new ChordPlayer(gui, simpleChordGenerator.getFrequencies()).playPattern());
            }
            else {
                moveRight();
                frequencies.addAll(new ChordPlayer(gui, sweepGenerator.getFrequencies()).playPattern());
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
        SpectrumState spectrumState = spectrumManager.getSpectrumState();
        Buckets origNoteBuckets;
        try {
            origNoteBuckets = spectrumState.noteBuckets;
        }
        catch(NullPointerException e){
            origNoteBuckets = new Buckets();
        }

        simpleChordGenerator.noteHistory = simpleChordGenerator.noteHistory.addNewBuckets(origNoteBuckets);
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
        Frequency previousFrequency = gui.spectrumWindow.getFrequency(gui.getX(sweepGenerator.getFrequencies()[0]) +
                simpleChordGenerator.margin + 1);
        return new SimpleChordGenerator(gui,
                spectrumManager,
                1,
                                  previousFrequency,
                                  totalMargin,
                gui.spectrumWindow.getX(previousFrequency),
                gui.spectrumWindow.getX(gui.spectrumWindow.upperBound), 1);
    }
}
