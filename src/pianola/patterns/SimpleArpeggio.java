package pianola.patterns;

import gui.spectrum.SpectrumWindow;
import gui.buckets.Buckets;
import frequency.Frequency;
import main.BoundedBuffer;
import main.InputPort;
import pianola.chordgen.IncrementalChordGenerator;
import pianola.chordgen.SimpleChordGenerator;

import java.util.*;

public class SimpleArpeggio implements PianolaPattern {
    private final int chordSize;
    private final SimpleChordGenerator simpleChordGenerator;

    private ArpeggiateUp arpeggiateUp;

    private final InputPort<Buckets> notesInput;

    public SimpleArpeggio(BoundedBuffer<Buckets> notesBuffer, BoundedBuffer<Buckets> harmonicsBuffer, int chordSize, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin) {
        this.chordSize = chordSize;

        notesInput = new InputPort<>(notesBuffer);

        Frequency centerFrequency = spectrumWindow.getCenterFrequency();
        simpleChordGenerator = new IncrementalChordGenerator(
                                harmonicsBuffer,
                                chordSize,
                centerFrequency.divideBy(2.0),
                     spectrumWindow.getX(centerFrequency.multiplyBy(1.5)) -
                                    spectrumWindow.getX(centerFrequency),
                                spectrumWindow.getX(centerFrequency.divideBy(4.0)),
                                spectrumWindow.getX(centerFrequency.multiplyBy(4.0)), 3, spectrumWindow, inaudibleFrequencyMargin);
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
            Buckets notes = notesInput.consume();

            simpleChordGenerator.noteHistory = simpleChordGenerator.noteHistory.addNewBuckets(notes);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void generateNewChord() {
        simpleChordGenerator.generateChord();
        Frequency[] newFrequencies = simpleChordGenerator.getFrequencies();
        arpeggiateUp = new ArpeggiateUp(chordSize, 4, newFrequencies);
    }

}