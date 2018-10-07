package pianola;

import frequency.Frequency;

public class SweepToTarget extends Sweep {
    Frequency sourceFrequency;
    Frequency targetFrequency;
    int sourceAsInt;
    int targetAsInt;
    private int keyWidth;
    private double multiplier;

    public SweepToTarget(Pianola pianola, int size, Frequency centerFrequency, double multiplier) {
        super(pianola, size, centerFrequency);

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
        return new SimpleChordGenerator(gui,
                1,
                centerFrequency,
                totalMargin,
                gui.spectrumWindow.getX(gui.spectrumWindow.lowerBound),
                gui.spectrumWindow.getX(gui.spectrumWindow.upperBound.divideBy(multiplier)),
                2);
    }

    @Override
    protected void generateNewChord() {
        simpleChordGenerator.generateChord();
        sourceFrequency = simpleChordGenerator.getFrequencies()[0];
        sourceAsInt = gui.spectrumWindow.getX(sourceFrequency);
        targetFrequency = sourceFrequency.multiplyBy(multiplier);
        targetAsInt = gui.spectrumWindow.getX(targetFrequency);
        keyWidth = (targetAsInt -sourceAsInt)/size;
        moveRight();
    }

    @Override
    protected SimpleChordGenerator findNextSweepGenerator() {
        if(sequencer.i == sequencer.notesPerMeasure-1){
            return new StaticGenerator(gui, targetFrequency);
        }
        else {
            int center = (sourceAsInt + keyWidth * sequencer.i);
            int left = (int) (center - keyWidth / 2.);
            int right = (int) (center + keyWidth / 2.);
            return new SimpleChordGenerator(gui,
                    1,
                    gui.spectrumWindow.getFrequency((double) center),
                    totalMargin,
                    left,
                    right,
                    0);
        }
    }
}
