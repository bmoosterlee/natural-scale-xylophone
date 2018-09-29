package pianola;

import frequency.Frequency;
import gui.GUI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ArpeggiateUp implements PianolaPattern{
    final Sequencer sequencer;
    private final GUI gui;
    Integer[] frequencies;

    public ArpeggiateUp(GUI gui, int notesPerMeasure, int measuresTillReset, Integer[] frequencies) {
        this.gui = gui;
        sequencer = new Sequencer(notesPerMeasure, measuresTillReset);
        this.frequencies = frequencies;
    }

    @Override
    public Set<Frequency> playPattern() {
        Integer frequency = frequencies[sequencer.i];

        sequencer.tick();

        return new HashSet(Arrays.asList(new Frequency[]{gui.spectrumWindow.getFrequency((double) frequency)}));
    }

}