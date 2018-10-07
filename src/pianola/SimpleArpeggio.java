package pianola;

import gui.Buckets;
import gui.GUI;
import frequency.Frequency;
import gui.SpectrumState;

import java.util.*;

public class SimpleArpeggio implements PianolaPattern {
    protected GUI gui;
    int chordSize;
    final SimpleChordGenerator simpleChordGenerator;

    protected ArpeggiateUp arpeggiateUp;

    public SimpleArpeggio(Pianola pianola, int chordSize) {
        gui = pianola.getGui();
        this.chordSize = chordSize;

        Frequency centerFrequency = gui.spectrumWindow.getCenterFrequency();
        simpleChordGenerator = new IncrementalChordGenerator(gui,
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
        SpectrumState spectrumState = gui.spectrumState;
        Buckets origNoteBuckets = spectrumState.noteBuckets;
        simpleChordGenerator.noteHistory.addNewBuckets(origNoteBuckets);
    }

    protected void generateNewChord() {
        simpleChordGenerator.generateChord();
        Frequency[] newFrequencies = simpleChordGenerator.getFrequencies();
        arpeggiateUp = new ArpeggiateUp(gui, chordSize, 4, newFrequencies);
    }

}