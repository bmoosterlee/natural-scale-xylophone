package sound;

import component.buffer.BoundedBuffer;
import component.buffer.PipeCallable;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import spectrum.SpectrumWindow;

public class AmplitudeCalculator {
    public static PipeCallable<BoundedBuffer<Long, OrderStampedPacket<Long>>, BoundedBuffer<Double[], OrderStampedPacket<Double[]>>> buildPipe(SampleRate sampleRate, SpectrumWindow spectrumWindow) {
        return inputBuffer -> {
            int width = spectrumWindow.width;

            Wave[] waveTable = new Wave[width];
            for (int x = 0; x < width; x++) {
                Frequency frequency = spectrumWindow.getFrequency(x);
                waveTable[x] = new Wave(frequency, sampleRate);
            }

            return inputBuffer.performMethod(input -> {
                Double[] amplitudes = new Double[width];
                for(int i = 0; i< width; i++){
                    amplitudes[i] = waveTable[i].getAmplitude(input);
                }
                return amplitudes;
            }, 100, "calculate amplitude");
        };
    }
}
