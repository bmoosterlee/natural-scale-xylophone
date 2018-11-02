package pianola.patterns;


import frequency.Frequency;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import pianola.Sequencer;

import java.util.*;

public class SweepToTargetUpDown implements PianolaPattern {
    private final Sequencer sequencer;

    private final int halvedSize;
    private final SweepToTarget sweep;
    private final Frequency[] frequencies;

    public SweepToTargetUpDown(int size, Frequency centerFrequency, double multiplier, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin) {
        sequencer = new Sequencer(size, 1);

        halvedSize = (int) (Math.ceil(size/2.)+1);
        frequencies = new Frequency[halvedSize];
        sweep = new SweepToTarget(halvedSize, centerFrequency, multiplier, spectrumWindow, inaudibleFrequencyMargin);
    }

    @Override
    public Set<Frequency> playPattern(Buckets noteBuckets, Buckets harmonicsBuckets) {
        HashSet<Frequency> frequencySet;
        try {
            Frequency frequency;

            if (sequencer.i < halvedSize) {
                frequency = sweep.playPattern(noteBuckets, harmonicsBuckets).iterator().next();
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
