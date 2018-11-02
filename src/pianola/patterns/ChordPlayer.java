package pianola.patterns;

import frequency.Frequency;
import gui.buckets.Buckets;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChordPlayer implements PianolaPattern {
    private final Frequency[] chord;

    public ChordPlayer(Frequency[] chord) {
        this.chord = chord;
    }

    @Override
    public Set<Frequency> playPattern(Buckets noteBuckets, Buckets harmonicsBuckets) {
        Set<Frequency> frequencies = new HashSet<>();

        Collections.addAll(frequencies, chord);

        return frequencies;
    }
}