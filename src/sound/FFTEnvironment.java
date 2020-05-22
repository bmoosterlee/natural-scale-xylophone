package sound;

import component.buffer.*;
import spectrum.SpectrumWindow;

public class FFTEnvironment {

    static int fftN;
    static int fftNLowerBound;
    static int diff;
    public static int resamplingWindow;

    public FFTEnvironment(SampleRate sampleRate){
        {
            int k = 0;
            do {
                fftN = (int) Math.pow(2, k);
                k++;
//                        } while (fftN / 2 < spectrumWindow.upperBound.getValue());
            } while (fftN < sampleRate.sampleRate);

//                        k = 0;
//                        do {
//                            fftNLowerBound = (int) Math.pow(2, k);
//                            resamplingWindow = fftN / fftNLowerBound;
//                            k++;
//                        } while (fftNLowerBound * 2 * 16 < spectrumWindow.lowerBound.getValue() && resamplingWindow > 512);

            fftNLowerBound = 1;
//                    fftNLowerBound = 32;

            //                Using a resampling window, the magnitude at i = 0 is the lower bound frequency,
            //                The magnitude at i = resamplingWindow is fftN.
            //                To convert from frequency to i we do the following:
            //                i = (f-fftNLowerBound)/diff * resamplingWindow;
            diff = fftN - fftNLowerBound;
            resamplingWindow = fftN / fftNLowerBound;
        }
    }

    public PipeCallable<BoundedBuffer<Double, SimplePacket<Double>>, BoundedBuffer<Complex[], SimplePacket<Complex[]>>> buildAudioInPipe(SpectrumWindow spectrumWindow, int sampleRate) {
        return inputBuffer ->
        {
            BoundedBuffer<Complex[], SimplePacket<Complex[]>> outputBuffer = new SimpleBuffer<>(1, "FFT input");

            new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {
                long sampleCount = 0;
                long lastSample = -1;
                double[] history;
                int count = 0;

                {
//                    Double[] oldMagnitudes;
//                    Double[] magnitudes;

                    history = new double[resamplingWindow];
//                        oldMagnitudes = new Double[history.length];
//                        magnitudes = new Double[history.length];
//                        for (int i = 0; i < history.length; i++) {
//                            magnitudes[i] = 0.;
//                            oldMagnitudes[i] = 0.;
//                        }
                }

                @Override
                public void tick() {
                    double in = 0;
                    try {
                        in = input.consume().unwrap();

                        if (count >= history.length) {
                            count = 0;

                            Complex[] magnitudeSpectrum = CalculateFFT.calculateFFT(history, history.length);

//                            oldMagnitudes = magnitudes;
//                            magnitudes = new Double[history.length];
//                            for(int i = 0; i<history.length; i++){
//                                magnitudes[i] = magnitudeSpectrum[i];
//                            }

                            output.produce(new SimplePacket<>(magnitudeSpectrum));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    long adjustedSampleCount = (long) ((double) sampleCount / sampleRate * fftN);
                    if (adjustedSampleCount != lastSample) {
                        lastSample = adjustedSampleCount;

                        history[count] = in;
                        count++;
                    }

                    sampleCount++;

//                        Double[] interpolatedMagnitudes = new Double[history.length];
//                        double completion = (double) count / (history.length - 1);
//
//                        for (int i = 0; i < history.length; i++) {
//                            interpolatedMagnitudes[i] = (1. - completion) * oldMagnitudes[i] + completion * magnitudes[i];
//                        }
//
//                        return interpolatedMagnitudes;
                }
            });
            return outputBuffer;
        };
    }

    public static double getMagnitudeSpectrumI(double frequency) {
        return (frequency - fftNLowerBound) / diff * resamplingWindow;
    }

    public PipeCallable<BoundedBuffer<Complex[], SimplePacket<Complex[]>>, BoundedBuffer<Double, SimplePacket<Double>>> buildAudioOutPipe(SampleRate sampleRate) {
        return inputBuffer -> {
            BoundedBuffer<Double, SimplePacket<Double>> outputBuffer = new SimpleBuffer<>(1, "FFT output");

            new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {

                long sampleCount = 0;
                double lastSample = -1;
                double[] results = new double[0];
                int count = 0;

                Double oldValue;
                Double newValue = 0.;

                @Override
                public void tick() {
                    try {
                        if (count >= results.length) {
                            count = 0;

                            Complex[] magnitudeSpectrum = input.consume().unwrap();

                            results = CalculateFFT.calculateIFFT(magnitudeSpectrum, magnitudeSpectrum.length);
                        }

                        double adjustedSampleCount = ((double) sampleCount / sampleRate.sampleRate * fftN);
                        if ((long) adjustedSampleCount != (long) lastSample) {
                            lastSample = adjustedSampleCount;

                            oldValue = newValue;
                            newValue = results[count];
                            count++;
                        }

                        double completion = (double) sampleCount / sampleRate.sampleRate * fftN - lastSample;

                        Double interpolatedValue = (1. - completion) * oldValue + completion * newValue;

                        sampleCount++;

                        output.produce(new SimplePacket<>(interpolatedValue));

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            });

            return outputBuffer;
        };
    }
}
