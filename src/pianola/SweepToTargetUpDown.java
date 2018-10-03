package pianola;


import frequency.Frequency;

import java.util.*;

public class SweepToTargetUpDown implements PianolaPattern {
    Sequencer sequencer;

    int halvedSize;
    SweepToTarget sweep;
    Frequency[] frequencies;

    public SweepToTargetUpDown(Pianola pianola, int size, Frequency centerFrequency, double multiplier) {
        sequencer = new Sequencer(size, 1);

        halvedSize = (int) (Math.ceil(size/2.)+1);
        frequencies = new Frequency[halvedSize];
        sweep = new SweepToTarget(pianola, halvedSize, centerFrequency, multiplier);
    }

    @Override
    public Set<Frequency> playPattern() {
        HashSet<Frequency> frequencySet;
        try {
            Frequency frequency;

            if (sequencer.i < halvedSize) {
                frequency = sweep.playPattern().iterator().next();
                frequencies[sequencer.i] = frequency;
            } else {
                int i = (halvedSize - 1) - (sequencer.i - (halvedSize - 1));
                frequency = frequencies[i];
            }

            sequencer.tick();
            frequencySet = new HashSet<>(Collections.singletonList(frequency));
        }
        catch(NoSuchElementException e){
            frequencySet = new HashSet<>();
        }

        return frequencySet;
    }
}
