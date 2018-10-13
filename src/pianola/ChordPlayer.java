package pianola;

import frequency.Frequency;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChordPlayer implements PianolaPattern{
    private final Frequency[] chord;

    public ChordPlayer(Frequency[] chord) {
        this.chord = chord;
    }

    @Override
    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies = new HashSet<>();

        Collections.addAll(frequencies, chord);

        return frequencies;
    }
}