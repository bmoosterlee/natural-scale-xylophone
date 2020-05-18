package spectrum;

import component.buffer.*;
import frequency.Frequency;
import sound.FFTEnvironment;

import java.util.AbstractMap;
import java.util.HashMap;

public class SpectrumWindow {
    private final Frequency centerFrequency = new Frequency(2 * 261.63);
    public final int width;
    public final Frequency lowerBound;
    public final Frequency upperBound;
    private final double logFrequencyMultiplier;
    private final double logFrequencyAdditive;
    private final double xMultiplier;

    public final HashMap<Integer, Frequency> staticFrequencyWindow;

    private double[] bottomFrequencies;
    private double[] topFrequencies;

    public SpectrumWindow(int width, double octaveRange) {
        this.width = width;

        lowerBound = centerFrequency.divideBy(Math.pow(2, octaveRange / 2));
        upperBound = centerFrequency.multiplyBy(Math.pow(2, octaveRange / 2));

        double logLowerBound = Math.log(lowerBound.getValue());
        double logUpperBound = Math.log(upperBound.getValue());
        double logRange = logUpperBound - logLowerBound;
        logFrequencyMultiplier = this.width / logRange;
        logFrequencyAdditive = logLowerBound * this.width / logRange;
        xMultiplier = logRange / this.width;

        staticFrequencyWindow = new HashMap<>();
        for(int x = 0; x < width; x++){
            staticFrequencyWindow.put(x, getFrequency(x));
        }

        bottomFrequencies = new double[this.width];
        topFrequencies = new double[this.width];
        for (int i = 0; i < width; i++) {
            bottomFrequencies[i] = getFrequency(i).getValue();
            topFrequencies[i] = getFrequency(i + 1).getValue();
        }
    }

    public int getX(Frequency frequency) {
        return (int) (Math.log(frequency.getValue()) * logFrequencyMultiplier - logFrequencyAdditive);
    }

    public boolean inBounds(Frequency frequency) {
        return inBounds(getX(frequency));
    }

    public boolean inBounds(Integer x) {
        return x >= 0 && x < width;
    }

    public Frequency getFrequency(double x) {
        //todo precalculate the multiplier for each x (not xMultiplier), and precalc the frequency for each x
        return lowerBound.multiplyBy(Math.exp(x * xMultiplier));
    }

    public Frequency getCenterFrequency() {
        return centerFrequency;
    }

    <V, Y extends Packet<AbstractMap.SimpleImmutableEntry<Integer, V>>> PipeCallable<BoundedBuffer<AbstractMap.SimpleImmutableEntry<Integer, V>, Y>, BoundedBuffer<AbstractMap.SimpleImmutableEntry<Integer, V>, Y>> buildInBoundsFilterPipe(int capacity) {
        return inputBuffer1 -> {
            SimpleBuffer<AbstractMap.SimpleImmutableEntry<Integer, V>, Y> outputBuffer = new SimpleBuffer<>(capacity, "spectrumBuilder - in bounds filter");

            new TickRunningStrategy(
                    new AbstractPipeComponent<>(inputBuffer1.createInputPort(), outputBuffer.createOutputPort()) {
                        @Override
                        protected void tick() {
                            try {
                                Y consumed = input.consume();
                                if (inBounds(consumed.unwrap().getKey())) {
                                    output.produce(consumed);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });

            return outputBuffer;
        };
    }

    public Double[] toSpectrumWindow(Double[] magnitudeSpectrum) {
        Double[] magnitudes = new Double[width];

        for (int i = 0; i < width; i++) {
            int bottomIndex = (int) FFTEnvironment.getMagnitudeSpectrumI(bottomFrequencies[i]);
            int topIndex = (int) FFTEnvironment.getMagnitudeSpectrumI(topFrequencies[i]);

            double bottomFraction = 1. - (FFTEnvironment.getMagnitudeSpectrumI(bottomFrequencies[i]) - bottomIndex);
            double bottomValue0 = magnitudeSpectrum[bottomIndex];
            double bottomValue1 = magnitudeSpectrum[bottomIndex + 1];
            double bottomValue = bottomValue1 - bottomFraction * (bottomValue1 - bottomValue0);

            double valuesInBetween = 0.;
            for (int j = bottomIndex + 1; j < topIndex; j++) {
                valuesInBetween += magnitudeSpectrum[j];
            }

            double topFraction = FFTEnvironment.getMagnitudeSpectrumI(topFrequencies[i]) - topIndex;
            double topValue0 = magnitudeSpectrum[topIndex];
            double topValue1 = magnitudeSpectrum[topIndex + 1];
            double topValue = topValue0 + topFraction * (topValue1 - topValue0);

            magnitudes[i] = (bottomValue + valuesInBetween + topValue);
        }

        return magnitudes;
    }
}