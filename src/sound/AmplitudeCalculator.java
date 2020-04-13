package sound;

import component.buffer.BoundedBuffer;
import component.buffer.PipeCallable;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import spectrum.SpectrumWindow;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.stream.Collectors;

public class AmplitudeCalculator {
    public static PipeCallable<BoundedBuffer<Long, OrderStampedPacket<Long>>, BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>>> buildPipe(SampleRate sampleRate, SpectrumWindow spectrumWindow) {
        return inputBuffer -> {
            HashMap<Frequency, Wave> waveTable = new HashMap<>();
            for (int x = 0; x < spectrumWindow.width; x++) {
                Frequency frequency = spectrumWindow.staticFrequencyWindow.get(x);
                waveTable.put(frequency, new Wave(frequency, sampleRate));
            }

            return inputBuffer.performMethod(input -> new AmplitudeState(new HashMap<>(waveTable.entrySet().stream().map(input0 -> new AbstractMap.SimpleImmutableEntry<>(input0.getKey(), input0.getValue().getAmplitude(input))).collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)))), 100, "calculate amplitude");
        };
    }
}
