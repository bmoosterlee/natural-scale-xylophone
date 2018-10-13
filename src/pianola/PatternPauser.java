package pianola;

import frequency.Frequency;

import java.util.HashSet;
import java.util.Set;

public class PatternPauser implements PianolaPattern {
    private final PianolaPattern pianolaPattern;
    private final int patternSize;
    private final Sequencer sequencer;

    PatternPauser(int size, PianolaPattern pianolaPattern, int patternSize){
        this.pianolaPattern = pianolaPattern;
        this.patternSize = patternSize;
        sequencer = new Sequencer(size, 1);
    }

    @Override
    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies;

        if(sequencer.i<patternSize){
            frequencies = pianolaPattern.playPattern();
        }
        else{
            frequencies = new HashSet<>();
        }

        sequencer.tick();
        return frequencies;
    }
}
