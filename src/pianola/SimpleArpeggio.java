package pianola;

import gui.spectrum.state.SpectrumManager;
import gui.buckets.Buckets;
import gui.GUI;
import frequency.Frequency;
import gui.spectrum.state.SpectrumState;

import java.util.*;

public class SimpleArpeggio implements PianolaPattern {
    protected GUI gui;
    SpectrumManager spectrumManager;
    int chordSize;
    final SimpleChordGenerator simpleChordGenerator;

    protected ArpeggiateUp arpeggiateUp;

    public SimpleArpeggio(Pianola pianola, SpectrumManager spectrumManager, int chordSize) {
        gui = pianola.getGui();
        this.spectrumManager = spectrumManager;
        this.chordSize = chordSize;

        Frequency centerFrequency = gui.spectrumWindow.getCenterFrequency();
        simpleChordGenerator = new IncrementalChordGenerator(gui,
                                spectrumManager,
                                chordSize,
                                centerFrequency.divideBy(2.0),
                     gui.spectrumWindow.getX(centerFrequency.multiplyBy(1.5)) -
                                    gui.spectrumWindow.getX(centerFrequency),
                                gui.spectrumWindow.getX(centerFrequency.divideBy(4.0)),
                                gui.spectrumWindow.getX(centerFrequency.multiplyBy(4.0)), 3);
        try {
            generateNewChord();
        }
        catch(NullPointerException e){

        }
    }

    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies = new HashSet<>();

        try {
            updateNoteBuckets();
        }
        catch(NullPointerException e){

        }

        try {
            if (arpeggiateUp.sequencer.j == 0 && arpeggiateUp.sequencer.i == 0) {
                generateNewChord();
            }
        }
        catch(NullPointerException e){
            try {
                generateNewChord();
            }
            catch(NullPointerException e2){

            }
        }

        try {
            frequencies.addAll(arpeggiateUp.playPattern());
        }
        catch(NullPointerException e){

        }

        return frequencies;
    }

    private void updateNoteBuckets() {
        SpectrumState spectrumState = spectrumManager.getSpectrumState();
        Buckets origNoteBuckets = spectrumState.noteBuckets;
        simpleChordGenerator.noteHistory = simpleChordGenerator.noteHistory.addNewBuckets(origNoteBuckets);
    }

    protected void generateNewChord() {
        simpleChordGenerator.generateChord();
        Frequency[] newFrequencies = simpleChordGenerator.getFrequencies();
        arpeggiateUp = new ArpeggiateUp(gui, chordSize, 4, newFrequencies);
    }

}