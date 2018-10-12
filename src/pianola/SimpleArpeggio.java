package pianola;

import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumManager;
import gui.buckets.Buckets;
import gui.GUI;
import frequency.Frequency;
import gui.spectrum.state.SpectrumState;

import java.util.*;

public class SimpleArpeggio implements PianolaPattern {
    SpectrumManager spectrumManager;
    int chordSize;
    final SimpleChordGenerator simpleChordGenerator;

    protected ArpeggiateUp arpeggiateUp;

    public SimpleArpeggio(SpectrumManager spectrumManager, int chordSize, SpectrumWindow spectrumWindow) {
        this.spectrumManager = spectrumManager;
        this.chordSize = chordSize;

        Frequency centerFrequency = spectrumWindow.getCenterFrequency();
        simpleChordGenerator = new IncrementalChordGenerator(
                                spectrumManager,
                                chordSize,
                                centerFrequency.divideBy(2.0),
                     spectrumWindow.getX(centerFrequency.multiplyBy(1.5)) -
                                    spectrumWindow.getX(centerFrequency),
                                spectrumWindow.getX(centerFrequency.divideBy(4.0)),
                                spectrumWindow.getX(centerFrequency.multiplyBy(4.0)), 3, spectrumWindow);
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
        arpeggiateUp = new ArpeggiateUp(chordSize, 4, newFrequencies);
    }

}