package pianola;

import frequency.Frequency;
import gui.spectrum.state.SpectrumManager;
import gui.spectrum.SpectrumWindow;

public class SweepToTarget extends Sweep {
    Frequency sourceFrequency;
    Frequency targetFrequency;
    int sourceAsInt;
    int targetAsInt;
    private int keyWidth;
    private double multiplier;

    public SweepToTarget(SpectrumManager spectrumManager, int size, Frequency centerFrequency, double multiplier, SpectrumWindow spectrumWindow) {
        super(spectrumManager, size, centerFrequency, spectrumWindow);

        this.multiplier = multiplier;

        //todo implement a left and right border to the sweep, so that we can play multiple sweeps at the same time
        // todo at different ranges
        simpleChordGenerator =
                getSimpleChordGenerator(centerFrequency);
        try {
            generateNewChord();
        }
        catch(NullPointerException e){

        }
    }

    @Override
    protected SimpleChordGenerator getSimpleChordGenerator(Frequency centerFrequency) {
        return new SimpleChordGenerator(
                spectrumManager,
                1,
                centerFrequency,
                totalMargin,
                spectrumWindow.getX(spectrumWindow.lowerBound),
                spectrumWindow.getX(spectrumWindow.upperBound.divideBy(multiplier)),
                2, spectrumWindow);
    }

    @Override
    protected void generateNewChord() {
        simpleChordGenerator.generateChord();
        sourceFrequency = simpleChordGenerator.getFrequencies()[0];
        sourceAsInt = spectrumWindow.getX(sourceFrequency);
        targetFrequency = sourceFrequency.multiplyBy(multiplier);
        targetAsInt = spectrumWindow.getX(targetFrequency);
        keyWidth = (targetAsInt -sourceAsInt)/size;
        moveRight();
    }

    @Override
    protected SimpleChordGenerator findNextSweepGenerator() {
        if(sequencer.i == sequencer.notesPerMeasure-1){
            return new StaticGenerator(spectrumManager, targetFrequency, spectrumWindow);
        }
        else {
            int center = (sourceAsInt + keyWidth * sequencer.i);
            int left = (int) (center - keyWidth / 2.);
            int right = (int) (center + keyWidth / 2.);
            return new SimpleChordGenerator(
                    spectrumManager,
                    1,
                    spectrumWindow.getFrequency((double) center),
                    totalMargin,
                    left,
                    right,
                    0, spectrumWindow);
        }
    }
}
