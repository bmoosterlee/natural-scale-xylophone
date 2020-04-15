package spectrum;

import component.buffer.BoundedBuffer;
import component.buffer.Packet;
import component.buffer.PipeCallable;
import component.buffer.SimplePacket;
import frequency.Frequency;
import sound.SampleRate;
import sound.VolumeStateMap;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SpectrumBuilder {

    public static <B extends Packet<Double[]>> BoundedBuffer<Double[], SimplePacket<Double[]>> buildHarmonicSpectrumPipe(BoundedBuffer<Double[], B> volumeBuffer, SpectrumWindow spectrumWindow, SampleRate sampleRate) {
        int maxHarmonics = 1000;

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

        double magnitude = Arrays.stream(harmonics[spectrumWindow.width/2]).reduce(0., Double::sum);
        if(magnitude!=0.) {
            for (int i = 0; i < spectrumWindow.width; i++) {
                for (int j = 0; j < spectrumWindow.width; j++) {
                    harmonics[i][j] /= magnitude;
                }
            }
        }

        return volumeBuffer.performMethod(input -> {
            Double[] harmonicsForThisVolumeSpectrum = new Double[spectrumWindow.width];
            for(int i = 0; i<spectrumWindow.width; i++){
                harmonicsForThisVolumeSpectrum[i] = 0.;
            }
            Double volumeBucket;
            for(int i = 0; i<spectrumWindow.width; i++){
                volumeBucket = input[i];
                if(volumeBucket == 0.){
                    continue;
                }
                for(int j = 0; j<spectrumWindow.width; j++) {
                    harmonicsForThisVolumeSpectrum[j] += volumeBucket * harmonics[i][j];
                }
            }
            return harmonicsForThisVolumeSpectrum;
        }, sampleRate.sampleRate / 32, "spectrum builder - calculate harmonics");
    }

}