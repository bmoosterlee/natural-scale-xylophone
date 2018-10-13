package pianola;

import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumManager;
import gui.buckets.Buckets;
import gui.GUI;
import frequency.Frequency;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;
import main.InputPort;

import java.util.*;

public class SimpleArpeggio implements PianolaPattern {
    int chordSize;
    final SimpleChordGenerator simpleChordGenerator;

    protected ArpeggiateUp arpeggiateUp;

    InputPort<SpectrumState> spectrumStateInput;

    public SimpleArpeggio(BoundedBuffer<SpectrumState> buffer, int chordSize, SpectrumWindow spectrumWindow) {
        this.chordSize = chordSize;

        spectrumStateInput = new InputPort<>(buffer);

        Frequency centerFrequency = spectrumWindow.getCenterFrequency();
        simpleChordGenerator = new IncrementalChordGenerator(
                                buffer,
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
        try {
            SpectrumState spectrumState = spectrumStateInput.consume();

            Buckets origNoteBuckets = spectrumState.noteBuckets;
            simpleChordGenerator.noteHistory = simpleChordGenerator.noteHistory.addNewBuckets(origNoteBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void generateNewChord() {
        simpleChordGenerator.generateChord();
        Frequency[] newFrequencies = simpleChordGenerator.getFrequencies();
        arpeggiateUp = new ArpeggiateUp(chordSize, 4, newFrequencies);
    }

}