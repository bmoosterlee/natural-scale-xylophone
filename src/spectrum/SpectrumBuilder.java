package spectrum;

import component.Pulse;
import component.TimedConsumer;
import component.buffer.*;
import frequency.Frequency;
import sound.VolumeState;
import sound.VolumeStateMap;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class SpectrumBuilder {

    public static <A extends Packet<Double[]>, B extends Packet<VolumeState>> SimpleImmutableEntry<BoundedBuffer<Double[], A>, BoundedBuffer<Double[], SimplePacket<Double[]>>> buildComponent(BoundedBuffer<Pulse, ? extends Packet<Pulse>> frameTickBuffer, BoundedBuffer<VolumeState, B> inputBuffer, SpectrumWindow spectrumWindow) {
        BufferChainLink<VolumeState, B> volumeStatePacketBufferChainLink = frameTickBuffer
                .performMethod(TimedConsumer.consumeFrom(inputBuffer), "spectrum builder - consume from volume state buffer");
        LinkedList<BoundedBuffer<VolumeState, B>> volumeBroadcast =
                new LinkedList<>(
                        volumeStatePacketBufferChainLink
                                .broadcast(2, "spectrum builder - volume broadcast"));

        return new SimpleImmutableEntry<>(
                buildNoteSpectrumPipe(volumeBroadcast.poll(), spectrumWindow),
                buildHarmonicSpectrumPipe(volumeBroadcast.poll(), spectrumWindow));
    }

    private static <A extends Packet<Double[]>, B extends Packet<VolumeState>> BufferChainLink<Double[], A> buildNoteSpectrumPipe(BoundedBuffer<VolumeState, B> inputBuffer, SpectrumWindow spectrumWindow) {
        return inputBuffer
                .performMethod(input -> input.volumes, "build note spectrum");
    }

    private static <B extends Packet<VolumeState>, C extends Packet<VolumeStateMap>> BoundedBuffer<Double[], SimplePacket<Double[]>> buildHarmonicSpectrumPipe(BoundedBuffer<VolumeState, B> volumeBuffer, SpectrumWindow spectrumWindow) {
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
                    harmonicsForThisVolumeSpectrum[j] += input.volumes[i] * harmonics[i][j];
                }
            }
            return harmonicsForThisVolumeSpectrum;
        }, 200, "spectrum builder - calculate harmonics");
    }

}