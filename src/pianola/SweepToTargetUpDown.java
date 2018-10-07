package pianola;


import frequency.Frequency;
import gui.SpectrumManager;

import java.util.*;

public class SweepToTargetUpDown implements PianolaPattern {
    SpectrumManager spectrumManager;
    Sequencer sequencer;

    int halvedSize;
    SweepToTarget sweep;
    Frequency[] frequencies;

    public SweepToTargetUpDown(Pianola pianola, SpectrumManager spectrumManager, int size, Frequency centerFrequency, double multiplier) {
        this.spectrumManager = spectrumManager;
        sequencer = new Sequencer(size, 1);

        halvedSize = (int) (Math.ceil(size/2.)+1);
        frequencies = new Frequency[halvedSize];
        sweep = new SweepToTarget(pianola, spectrumManager, halvedSize, centerFrequency, multiplier);
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
