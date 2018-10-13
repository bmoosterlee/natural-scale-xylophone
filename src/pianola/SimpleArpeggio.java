package pianola;

import gui.spectrum.SpectrumWindow;
import gui.buckets.Buckets;
import frequency.Frequency;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;
import main.InputPort;

import java.util.*;

public class SimpleArpeggio implements PianolaPattern {
    private final int chordSize;
    final SimpleChordGenerator simpleChordGenerator;

    ArpeggiateUp arpeggiateUp;

    private final InputPort<SpectrumState> spectrumStateInput;

    SimpleArpeggio(BoundedBuffer<SpectrumState> buffer, int chordSize, SpectrumWindow spectrumWindow) {
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
        catch(NullPointerException ignored){

        }
    }

    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies = new HashSet<>();

        try {
            updateNoteBuckets();
        }
        catch(NullPointerException ignored){

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
            catch(NullPointerException ignored){

            }
        }

        try {
            frequencies.addAll(arpeggiateUp.playPattern());
        }
        catch(NullPointerException ignored){

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

    void generateNewChord() {
        simpleChordGenerator.generateChord();
        Frequency[] newFrequencies = simpleChordGenerator.getFrequencies();
        arpeggiateUp = new ArpeggiateUp(chordSize, 4, newFrequencies);
    }

}