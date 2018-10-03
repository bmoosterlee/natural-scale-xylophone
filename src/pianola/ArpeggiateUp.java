package pianola;

import frequency.Frequency;
import gui.GUI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ArpeggiateUp implements PianolaPattern{
    final Sequencer sequencer;
    private final GUI gui;
    Frequency[] frequencies;

    public ArpeggiateUp(GUI gui, int notesPerMeasure, int measuresTillReset, Frequency[] frequencies) {
        this.gui = gui;
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