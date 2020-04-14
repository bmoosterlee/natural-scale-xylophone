package spectrum;

import component.Pulse;
import component.TimedConsumer;
import component.buffer.BoundedBuffer;
import component.buffer.Packet;
import component.buffer.PipeCallable;
import component.buffer.SimplePacket;
import frequency.Frequency;
import sound.VolumeStateMap;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class SpectrumBuilder {

    public static <B extends Packet<Double[]>> SimpleImmutableEntry<BoundedBuffer<Double[], SimplePacket<Double[]>>, BoundedBuffer<Double[], SimplePacket<Double[]>>> buildComponent(BoundedBuffer<Pulse, ? extends Packet<Pulse>> frameTickBuffer, BoundedBuffer<Double[], B> inputBuffer, SpectrumWindow spectrumWindow) {
        BoundedBuffer<Double[], B> volumeStatePacketBufferChainLink = frameTickBuffer
                .performMethod(TimedConsumer.consumeFrom(inputBuffer), "spectrum builder - consume from volume state buffer");
        LinkedList<BoundedBuffer<Double[], B>> volumeBroadcast =
                new LinkedList<>(
                        volumeStatePacketBufferChainLink
                                .broadcast(2, "spectrum builder - volume broadcast"));

        return new SimpleImmutableEntry<>(
                volumeBroadcast.poll().rewrap(),
                buildHarmonicSpectrumPipe(volumeBroadcast.poll(), spectrumWindow));
    }

    private static <B extends Packet<Double[]>> BoundedBuffer<Double[], SimplePacket<Double[]>> buildHarmonicSpectrumPipe(BoundedBuffer<Double[], B> volumeBuffer, SpectrumWindow spectrumWindow) {
        int maxHarmonics = 200;

        Double[][] harmonics = new Double[spectrumWindow.width][spectrumWindow.width];
        PipeCallable<VolumeStateMap, Iterator<Map.Entry<Harmonic, Double>>> harmonicCalculator = HarmonicCalculator.calculateHarmonics(maxHarmonics);
        for (int i = 0; i < spectrumWindow.width; i++) {
            Double[] harmonicsForThisIndex = new Double[spectrumWindow.width];
            for (int j = 0; j < spectrumWindow.width; j++) {
                harmonicsForThisIndex[j] = 0.;
            }
            HashMap<Frequency, Double> tempBottom = new HashMap<>();
            tempBottom.put(spectrumWindow.getFrequency(i), 1.);
            HashMap<Frequency, Double> tempTop = new HashMap<>();
            tempTop.put(spectrumWindow.getFrequency(i+1), 1.);
            Iterator<Map.Entry<Harmonic, Double>> bottomHarmonicsIterator = harmonicCalculator.call(new VolumeStateMap(tempBottom));
            Iterator<Map.Entry<Harmonic, Double>> topHarmonicsIterator = harmonicCalculator.call(new VolumeStateMap(tempTop));
            while(bottomHarmonicsIterator.hasNext()) {
                Map.Entry<Harmonic, Double> bottomHarmonicWithVolume = bottomHarmonicsIterator.next();
                Map.Entry<Harmonic, Double> topHarmonicWithVolume = topHarmonicsIterator.next();
                Frequency bottomFrequency = bottomHarmonicWithVolume.getKey().getHarmonicFrequency();
                Frequency topFrequency = topHarmonicWithVolume.getKey().getHarmonicFrequency();
                int x0 = (spectrumWindow.getX(bottomFrequency));
                int x1 = (spectrumWindow.getX(topFrequency));
                for(int x = x0; x==x0 || x<x1; x++) {
                    if (x >= 0 && x < spectrumWindow.width) {
                        harmonicsForThisIndex[x] += bottomHarmonicWithVolume.getValue();
                    }
                }
            }
            harmonics[i] = harmonicsForThisIndex;
        }

        return volumeBuffer.performMethod(input -> {
            Double[] harmonicsForThisVolumeSpectrum = new Double[spectrumWindow.width];
            for(int i = 0; i<spectrumWindow.width; i++){
                harmonicsForThisVolumeSpectrum[i] = 0.;
            }
            for(int i = 0; i<spectrumWindow.width; i++){
                for(int j = 0; j<spectrumWindow.width; j++) {
                    harmonicsForThisVolumeSpectrum[j] += input[i] * harmonics[i][j];
                }
            }
            return harmonicsForThisVolumeSpectrum;
        }, 200, "spectrum builder - calculate harmonics");
    }

}