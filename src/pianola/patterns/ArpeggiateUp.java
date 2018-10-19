package pianola.patterns;

import frequency.Frequency;
import pianola.Sequencer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ArpeggiateUp implements PianolaPattern {
    final Sequencer sequencer;
    private final Frequency[] frequencies;

    ArpeggiateUp(int notesPerMeasure, int measuresTillReset, Frequency[] frequencies) {
        sequencer = new Sequencer(notesPerMeasure, measuresTillReset);
        this.frequencies = frequencies;
    }

    @Override
    public Set<Frequency> playPattern() {
        Frequency frequency = frequencies[sequencer.i];

        sequencer.tick();

        return new HashSet<>(Collections.singletonList(frequency));
    }

}