package pianola;

import frequency.Frequency;
import gui.GUI;

import java.util.HashSet;
import java.util.Set;

public class ChordPlayer implements PianolaPattern{
    GUI gui;
    Integer[] chord;

    public ChordPlayer(GUI gui, Integer[] chord) {
        this.gui = gui;
        this.chord = chord;
    }

    @Override
    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies = new HashSet<>();

        for (Integer x : chord) {
            frequencies.add(gui.spectrumWindow.getFrequency((double) x));
        }

        return frequencies;
    }
}