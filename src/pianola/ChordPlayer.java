package pianola;

import frequency.Frequency;
import gui.GUI;

import java.util.HashSet;
import java.util.Set;

public class ChordPlayer implements PianolaPattern{
    Frequency[] chord;

    public ChordPlayer(Frequency[] chord) {
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