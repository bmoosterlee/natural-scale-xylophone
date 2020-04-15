package sound;

import component.buffer.PipeCallable;
import spectrum.SpectrumWindow;

public class FFTEnvironment {

    public static PipeCallable<Double, Double[]> buildPipe(int sampleRate, SpectrumWindow spectrumWindow, final double gain) {
        return new PipeCallable<>() {
            int fftN;

            double[] history;

            double[] bottomFrequencies = new double[spectrumWindow.width];
            double[] topFrequencies = new double[spectrumWindow.width];

            long sampleCount = 0;
            long lastSample = 0;

            {
                for (int i = 0; i < spectrumWindow.width; i++) {
                    bottomFrequencies[i] = spectrumWindow.getFrequency(i).getValue();
                    topFrequencies[i] = spectrumWindow.getFrequency(i + 1).getValue();
                }

                int i = 0;
                do {
                    fftN = (int) Math.pow(2, i);
                    i++;
                } while (fftN / 2 < spectrumWindow.upperBound.getValue());

                history = new double[fftN];
            }

            @Override
            public Double[] call(Double input) {
                long adjustedSampleCount = (long) ((double) (sampleCount % sampleRate) / sampleRate * fftN);
                if(adjustedSampleCount != lastSample) {
                    lastSample = adjustedSampleCount;

                    double[] newHistory = new double[fftN];
                    System.arraycopy(history, 1, newHistory, 0, fftN - 1);
                    newHistory[fftN - 1] = input;
                    history = newHistory;
                }
                sampleCount++;

                double[] magnitudeSpectrum = CalculateFFT.calculateFFT(history, fftN);

                Double[] magnitudes = new Double[spectrumWindow.width];
                for(int i = 0; i<spectrumWindow.width; i++){
                    int bottomIndex = (int) bottomFrequencies[i];
                    int topIndex = (int) topFrequencies[i];

                    double bottomFraction = 1. - (bottomFrequencies[i] - bottomIndex);
                    double bottomValue0 = magnitudeSpectrum[bottomIndex];
                    double bottomValue1 = magnitudeSpectrum[bottomIndex + 1];
                    double bottomValue = bottomValue0 + bottomFraction * (bottomValue1 - bottomValue0);

                    double valuesInBetween = 0.;
                    for(int j = bottomIndex + 1; j < topIndex; j++){
                        valuesInBetween += magnitudeSpectrum[j];
                    }

                    double topFraction = topFrequencies[i] - topIndex;
                    double topValue0 = magnitudeSpectrum[topIndex];
                    double topValue1 = magnitudeSpectrum[topIndex + 1];
                    double topValue = topValue0 + topFraction * (topValue1 - topValue0);

                    magnitudes[i] = (bottomValue + valuesInBetween + topValue) * gain;
                }
                return magnitudes;
            }
        };
    }
}
