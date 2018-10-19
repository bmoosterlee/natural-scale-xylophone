package pianola.patterns;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import main.BoundedBuffer;
import pianola.chordgen.SimpleChordGenerator;
import pianola.chordgen.StaticGenerator;

class SweepToTarget extends Sweep {
    private Frequency targetFrequency;
    private int sourceAsInt;
    private int keyWidth;
    private final double multiplier;

    public SweepToTarget(BoundedBuffer<Buckets> notesBuffer, BoundedBuffer<Buckets> harmonicsBuffer, int size, Frequency centerFrequency, double multiplier, SpectrumWindow spectrumWindow) {
        super(notesBuffer, harmonicsBuffer, size, centerFrequency, spectrumWindow);

        this.multiplier = multiplier;

        //todo implement a left and right border to the sweep, so that we can play multiple sweeps at the same time
        // todo at different ranges
        simpleChordGenerator =
                getSimpleChordGenerator(centerFrequency);
        try {
            generateNewChord();
        }
        catch(NullPointerException ignored){

        }
    }

    @Override
    SimpleChordGenerator getSimpleChordGenerator(Frequency centerFrequency) {
        return new SimpleChordGenerator(
                harmonicsBuffer,
                1,
                centerFrequency,
                totalMargin,
                spectrumWindow.getX(spectrumWindow.lowerBound),
                spectrumWindow.getX(spectrumWindow.upperBound.divideBy(multiplier)),
                2, spectrumWindow);
    }

    @Override
    void generateNewChord() {
        simpleChordGenerator.generateChord();
        Frequency sourceFrequency = simpleChordGenerator.getFrequencies()[0];
        sourceAsInt = spectrumWindow.getX(sourceFrequency);
        targetFrequency = sourceFrequency.multiplyBy(multiplier);
        int targetAsInt = spectrumWindow.getX(targetFrequency);
        keyWidth = (targetAsInt -sourceAsInt)/size;
        moveRight();
    }

    @Override
    protected SimpleChordGenerator findNextSweepGenerator() {
        if(sequencer.i == sequencer.notesPerMeasure-1){
            return new StaticGenerator(harmonicsBuffer, targetFrequency, spectrumWindow);
        }
        else {
            int center = (sourceAsInt + keyWidth * sequencer.i);
            int left = (int) (center - keyWidth / 2.);
            int right = (int) (center + keyWidth / 2.);
            return new SimpleChordGenerator(
                    harmonicsBuffer,
                    1,
                    spectrumWindow.getFrequency((double) center),
                    totalMargin,
                    left,
                    right,
                    0, spectrumWindow);
        }
    }
}
