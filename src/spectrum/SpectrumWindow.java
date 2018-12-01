package spectrum;

import component.buffer.*;
import frequency.Frequency;

import java.util.AbstractMap;

public class SpectrumWindow {
    private final Frequency centerFrequency = new Frequency(2 * 261.63);
    final int width;
    public final Frequency lowerBound;
    public final Frequency upperBound;
    private final double logFrequencyMultiplier;
    private final double logFrequencyAdditive;
    private final double xMultiplier;

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

    <V> PipeCallable<BoundedBuffer<AbstractMap.SimpleImmutableEntry<Integer, V>>, BoundedBuffer<AbstractMap.SimpleImmutableEntry<Integer, V>>> buildInBoundsFilterPipe() {
        return inputBuffer1 -> {
            SimpleBuffer<AbstractMap.SimpleImmutableEntry<Integer, V>> outputBuffer = new SimpleBuffer<>(1, "spectrumBuilder - in bounds filter");

            new TickRunningStrategy(
                    new AbstractPipeComponent<>(inputBuffer1.createInputPort(), outputBuffer.createOutputPort()) {
                        @Override
                        protected void tick() {
                            try {
                                AbstractMap.SimpleImmutableEntry<Integer, V> consumed = input.consume();
                                if (inBounds(consumed.getKey())) {
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
}