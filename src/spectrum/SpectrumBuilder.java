package spectrum;

import component.buffer.BoundedBuffer;
import component.buffer.PipeCallable;
import component.buffer.SimpleBuffer;
import component.buffer.SimplePacket;
import frequency.Frequency;
import sound.Complex;
import sound.FFTEnvironment;
import sound.VolumeStateMap;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.*;

public class SpectrumBuilder {

//    public static BoundedBuffer<Complex[], SimplePacket<Complex[]>> buildHarmonicSpectrumPipe(SimpleBuffer<Complex[], SimplePacket<Complex[]>> volumeBuffer) {
//        int maxHarmonics = 1;
//
//        Double[][] harmonics = new Double[FFTEnvironment.resamplingWindow][FFTEnvironment.resamplingWindow];
//        PipeCallable<VolumeStateMap, Iterator<Map.Entry<Harmonic, Double>>> harmonicCalculator = HarmonicCalculator.calculateHarmonics(maxHarmonics);
//        for (int i = 0; i < FFTEnvironment.resamplingWindow; i++) {
//            Double[] harmonicsForThisIndex = new Double[FFTEnvironment.resamplingWindow];
//            Arrays.fill(harmonicsForThisIndex, 0.);
//            HashMap<Frequency, Double> tempBottom = new HashMap<>();
//            tempBottom.put(new Frequency(i), 1.);
//            HashMap<Frequency, Double> tempTop = new HashMap<>();
//            tempTop.put(new Frequency(i+1), 1.);
//            Iterator<Map.Entry<Harmonic, Double>> bottomHarmonicsIterator = harmonicCalculator.call(new VolumeStateMap(tempBottom));
//            Iterator<Map.Entry<Harmonic, Double>> topHarmonicsIterator = harmonicCalculator.call(new VolumeStateMap(tempTop));
//
////            for(int k = 0; k<1; k++) {
////                bottomHarmonicsIterator.next();
////                topHarmonicsIterator.next();
////            }
//
//            while(bottomHarmonicsIterator.hasNext()) {
//                Map.Entry<Harmonic, Double> bottomHarmonicWithVolume = bottomHarmonicsIterator.next();
//                Map.Entry<Harmonic, Double> topHarmonicWithVolume = topHarmonicsIterator.next();
//                Frequency bottomFrequency = bottomHarmonicWithVolume.getKey().getHarmonicFrequency();
//                Frequency topFrequency = topHarmonicWithVolume.getKey().getHarmonicFrequency();
//                int x0 = (int) bottomFrequency.getValue();
//                int x1 = (int) topFrequency.getValue();
//                for(int x = x0; x==x0 || x<x1; x++) {
//                    if (x >= 0 && x < FFTEnvironment.resamplingWindow) {
//                        harmonicsForThisIndex[x] += bottomHarmonicWithVolume.getValue();
//                    }
//                }
//            }
//            harmonics[i] = harmonicsForThisIndex;
//        }
//
//        double magnitude = Arrays.stream(harmonics[FFTEnvironment.resamplingWindow/2]).reduce(0., Double::sum);
//        if(magnitude!=0.) {
//            for (int i = 0; i < FFTEnvironment.resamplingWindow; i++) {
//                Double[] harmonic = harmonics[i];
//                for (int j = 0; j < FFTEnvironment.resamplingWindow; j++) {
//                    harmonic[j] /= magnitude;
//                }
//            }
//        }
//
//        return volumeBuffer.performMethod(input -> {
//            Complex[] harmonicsForThisVolumeSpectrum = new Complex[FFTEnvironment.resamplingWindow];
//            Arrays.fill(harmonicsForThisVolumeSpectrum, new Complex(0., 0.));
//            Complex volumeBucket;
//            Double[] harmonic;
//            for(int i = 0; i<FFTEnvironment.resamplingWindow; i++){
//                volumeBucket = input[i];
//                harmonic = harmonics[i];
//                for(int j = 0; j<FFTEnvironment.resamplingWindow; j++) {
//                    harmonicsForThisVolumeSpectrum[j] = harmonicsForThisVolumeSpectrum[j].plus(volumeBucket.scale(harmonic[j]));
//                }
//            }
//            return harmonicsForThisVolumeSpectrum;
//        }, "spectrum builder - calculate harmonics");
//    }

    public static BoundedBuffer<Complex[], SimplePacket<Complex[]>> buildHarmonicSpectrumPipe(SimpleBuffer<Complex[], SimplePacket<Complex[]>> volumeBuffer) {
        int maxHarmonics = 20;

        PipeCallable<VolumeStateMap, Iterator<Map.Entry<Harmonic, Double>>> harmonicCalculator = HarmonicCalculator.calculateHarmonics(maxHarmonics);
        int counter = 0;

//        Skip tonic
        boolean tonicIncluded = false;
        int tonic;
        if(tonicIncluded) {
            tonic = 1;
        } else {
            tonic = 0;
        }
        int adjustedNumberOfHarmonics = tonic + maxHarmonics;
        Map.Entry<Double, Double>[] harmonics = new Map.Entry[adjustedNumberOfHarmonics];
        HashMap<Frequency, Double> tempMap = new HashMap<>();
        tempMap.put(new Frequency(1), 1.);
        Iterator<Map.Entry<Harmonic, Double>> harmonicsIterator = harmonicCalculator.call(new VolumeStateMap(tempMap));

        if(!tonicIncluded){
            harmonicsIterator.next();
        }

        while(harmonicsIterator.hasNext()) {
            Map.Entry<Harmonic, Double> harmonicWithVolume = harmonicsIterator.next();
            Frequency frequency = harmonicWithVolume.getKey().getHarmonicFrequency();
            int x = (int) frequency.getValue();
            harmonics[counter] = new AbstractMap.SimpleImmutableEntry<>(harmonicWithVolume.getKey().getHarmonicFrequency().getValue(), harmonicWithVolume.getValue());
            counter++;
        }

        double magnitude = Arrays.stream(harmonics).map(Map.Entry::getValue).reduce(0., Double::sum);
        if(magnitude!=0.) {
            for (int j = 0; j < harmonics.length; j++) {
                Map.Entry<Double, Double> harmonic = harmonics[j];
                harmonics[j] = new AbstractMap.SimpleImmutableEntry<>(harmonic.getKey(), harmonic.getValue()/magnitude);
            }
        }

        return volumeBuffer.performMethod(input -> {
            Complex[] harmonicsForThisVolumeSpectrum = new Complex[FFTEnvironment.resamplingWindow];
            Arrays.fill(harmonicsForThisVolumeSpectrum, new Complex(0., 0.));
            Complex volumeBucket;
            Map.Entry<Double, Double> harmonic;
            Double frequencyMultiplier;
            double newFrequency;
            int x0;
            int x1;
            double xFraction;
            Double volumeMultiplier;
            for(int i = 0; i<FFTEnvironment.resamplingWindow; i++){
                volumeBucket = input[i];
                for(int j = 0; j<harmonics.length; j++) {
                    harmonic = harmonics[j];
                    frequencyMultiplier = harmonic.getKey();
                    newFrequency = i * frequencyMultiplier;
                    x0 = (int) newFrequency;
                    x1 = x0 + 1;
                    xFraction = newFrequency - x0;
                    volumeMultiplier = harmonic.getValue();
                    if(x0 >= 0 && x0 < FFTEnvironment.resamplingWindow) {
                        harmonicsForThisVolumeSpectrum[x0] = harmonicsForThisVolumeSpectrum[x0].plus(volumeBucket.scale((1 - xFraction) * volumeMultiplier));
                    }
                    if(x1 >= 0 && x1 < FFTEnvironment.resamplingWindow) {
                        harmonicsForThisVolumeSpectrum[x1] = harmonicsForThisVolumeSpectrum[x1].plus(volumeBucket.scale(xFraction * volumeMultiplier));
                    }
                }
            }
            return harmonicsForThisVolumeSpectrum;
        }, "spectrum builder - calculate harmonics");
    }
}