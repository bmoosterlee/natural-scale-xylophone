package pianola;

import gui.Buckets;
import gui.GUI;
import frequency.Frequency;
import gui.SpectrumSnapshot;

import java.util.*;

public class SimpleArpeggio implements PianolaPattern {
    protected GUI gui;
    int chordSize;
    final SimpleChordGenerator simpleChordGenerator;

    protected ArpeggiateUp arpeggiateUp;

    public SimpleArpeggio(Pianola pianola, int chordSize) {
        gui = pianola.getGui();
        this.chordSize = chordSize;

        simpleChordGenerator =
                new IncrementalChordGenerator(gui,
                                              chordSize,
                                              gui.spectrumWindow.getCenterFrequency().divideBy(2.0),
                                   gui.spectrumWindow.getX(gui.spectrumWindow.getCenterFrequency().multiplyBy(1.5)) -
                                           gui.spectrumWindow.getX(gui.spectrumWindow.getCenterFrequency()),
                        gui.spectrumWindow.getX(gui.spectrumWindow.getCenterFrequency().divideBy(4.0)));
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
        SpectrumSnapshot spectrumSnapshot = gui.spectrumSnapshot;
        Buckets origNoteBuckets = spectrumSnapshot.noteBuckets;
        simpleChordGenerator.noteHistory.addNewBuckets(origNoteBuckets);
    }

    protected void generateNewChord() {
        simpleChordGenerator.generateChord();
        Frequency[] newFrequencies = simpleChordGenerator.getFrequencies();
        arpeggiateUp = new ArpeggiateUp(gui, chordSize, 4, newFrequencies);
    }

}