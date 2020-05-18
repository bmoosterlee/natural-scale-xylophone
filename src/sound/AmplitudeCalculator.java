package sound;

import component.buffer.BoundedBuffer;
import component.buffer.MethodOutputComponent;
import component.buffer.OutputCallable;
import component.buffer.SimplePacket;
import frequency.Frequency;
import spectrum.SpectrumWindow;

public class AmplitudeCalculator {
    public static MethodOutputComponent<Double[]> buildPipe(BoundedBuffer<Double[], SimplePacket<Double[]>> outputBuffer, SampleRate sampleRate, SpectrumWindow spectrumWindow) {
        return new MethodOutputComponent<>(outputBuffer, new OutputCallable<>() {

            int width = spectrumWindow.width;
            Wave[] waveTable = new Wave[width];

            {
                for (int x = 0; x < width; x++) {
                    Frequency frequency0 = spectrumWindow.getFrequency(x);
                    Frequency frequency1 = spectrumWindow.getFrequency(x + 1);
                    Frequency frequency = new Frequency((frequency0.getValue() + frequency1.getValue()) / 2.);
                    waveTable[x] = new Wave(frequency, sampleRate);
                }
            }

            long sampleCount = 0L;

            @Override
            public Double[] call() {
                Double[] amplitudes = new Double[width];
                double timeInSeconds = sampleRate.asTimeRaw(sampleCount);
                for (int i = 0; i < width; i++) {
                    amplitudes[i] = waveTable[i].getAmplitude(timeInSeconds);
                }
                sampleCount++;
                return amplitudes;
            }
        });
    }
}
