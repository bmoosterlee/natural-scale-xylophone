package pianola;

import frequency.Frequency;
import gui.GUI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ArpeggiateUp implements PianolaPattern{
    final Sequencer sequencer;
    Frequency[] frequencies;

    public ArpeggiateUp(int notesPerMeasure, int measuresTillReset, Frequency[] frequencies) {
        sequencer = new Sequencer(notesPerMeasure, measuresTillReset);
        this.frequencies = frequencies;
    }

    @Override
    public Set<Frequency> playPattern() {
        Frequency frequency = frequencies[sequencer.i];

        sequencer.tick();

        return new HashSet(Arrays.asList(new Frequency[]{frequency}));
    }

}