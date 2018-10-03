package pianola;

import frequency.Frequency;
import gui.GUI;

import java.util.HashSet;
import java.util.Set;

public class ChordPlayer implements PianolaPattern{
    GUI gui;
    Frequency[] chord;

    public ChordPlayer(GUI gui, Frequency[] chord) {
        this.gui = gui;
        this.chord = chord;
    }

    @Override
    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies = new HashSet<>();

        for (Frequency frequency : chord) {
            frequencies.add(frequency);
        }

        return frequencies;
    }
}