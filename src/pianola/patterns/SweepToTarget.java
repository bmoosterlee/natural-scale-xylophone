package pianola.patterns;

import frequency.Frequency;
import spectrum.buckets.Buckets;
import spectrum.SpectrumWindow;
import pianola.chordgen.SimpleChordGenerator;
import pianola.chordgen.StaticGenerator;

class SweepToTarget extends Sweep {
    private Frequency targetFrequency;
    private int sourceAsInt;
    private int keyWidth;
    private final double multiplier;

    public SweepToTarget(int size, Frequency centerFrequency, double multiplier, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin) {
        super(size, centerFrequency, spectrumWindow, inaudibleFrequencyMargin);

        this.multiplier = multiplier;

        //todo implement a left and right border to the sweep, so that we can play multiple sweeps at the same time
        // todo at different ranges
        simpleChordGenerator =
                getSimpleChordGenerator(centerFrequency);
    }

    @Override
    SimpleChordGenerator getSimpleChordGenerator(Frequency centerFrequency) {
        return new SimpleChordGenerator(
                1,
                centerFrequency,
                totalMargin,
                spectrumWindow.getX(spectrumWindow.lowerBound),
                spectrumWindow.getX(spectrumWindow.upperBound.divideBy(multiplier)),
                spectrumWindow);
    }

    @Override
    void generateNewChord(Buckets noteBuckets, Buckets harmonicsBuckets) {
        simpleChordGenerator.generateChord(noteBuckets, harmonicsBuckets);
        Frequency sourceFrequency = simpleChordGenerator.getFrequencies()[0];
        sourceAsInt = spectrumWindow.getX(sourceFrequency);
        targetFrequency = sourceFrequency.multiplyBy(multiplier);
        int targetAsInt = spectrumWindow.getX(targetFrequency);
        keyWidth = (targetAsInt -sourceAsInt)/size;
        moveRight(noteBuckets, harmonicsBuckets);
    }

    @Override
    protected SimpleChordGenerator findNextSweepGenerator() {
        if(sequencer.i == sequencer.notesPerMeasure-1){
            return new StaticGenerator(
                    targetFrequency,
                    spectrumWindow);
        }
        else {
            int center = (sourceAsInt + keyWidth * sequencer.i);
            int left = (int) (center - keyWidth / 2.);
            int right = (int) (center + keyWidth / 2.);
            return new SimpleChordGenerator(
                    1,
                    spectrumWindow.getFrequency((double) center),
                    totalMargin,
                    left,
                    right,
                    spectrumWindow
            );
        }
    }
}
