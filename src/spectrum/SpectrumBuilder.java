package spectrum;

import component.buffer.*;
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

    Map.Entry<Double, Double>[] harmonics;

    public SpectrumBuilder(int numberOfHarmonics, boolean tonicIncluded) {
        harmonics = calculateHarmonics(numberOfHarmonics, tonicIncluded);
    }

    private static Map.Entry<Double, Double>[] calculateHarmonics(int numberOfHarmonics, boolean tonicIncluded) {
        PipeCallable<VolumeStateMap, Iterator<Map.Entry<Harmonic, Double>>> harmonicCalculator = HarmonicCalculator.calculateHarmonics(numberOfHarmonics);
        int counter = 0;

//        Skip tonic
        int tonic;
        if (tonicIncluded) {
            tonic = 1;
        } else {
            tonic = 0;
        }
        int adjustedNumberOfHarmonics = tonic + numberOfHarmonics;
        Map.Entry<Double, Double>[] harmonics = new Map.Entry[adjustedNumberOfHarmonics];
        HashMap<Frequency, Double> tempMap = new HashMap<>();
        tempMap.put(new Frequency(1), 1.);
        Iterator<Map.Entry<Harmonic, Double>> harmonicsIterator = harmonicCalculator.call(new VolumeStateMap(tempMap));

        if (!tonicIncluded) {
            harmonicsIterator.next();
        }

        while (harmonicsIterator.hasNext()) {
            Map.Entry<Harmonic, Double> harmonicWithVolume = harmonicsIterator.next();
            harmonics[counter] = new AbstractMap.SimpleImmutableEntry<>(harmonicWithVolume.getKey().getHarmonicFrequency().getValue(), harmonicWithVolume.getValue());
            counter++;
        }

        double magnitude = Arrays.stream(harmonics).map(Map.Entry::getValue).reduce(0., Double::sum);
        if (magnitude != 0.) {
            for (int j = 0; j < harmonics.length; j++) {
                Map.Entry<Double, Double> harmonic = harmonics[j];
                harmonics[j] = new AbstractMap.SimpleImmutableEntry<>(harmonic.getKey(), harmonic.getValue() / magnitude);
            }
        }
        return harmonics;
    }

    public BoundedBuffer<Complex[], SimplePacket<Complex[]>> buildHarmonicSpectrumPipe(SimpleBuffer<Complex[], SimplePacket<Complex[]>> volumeBuffer) {
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
            for (int i = 0; i < FFTEnvironment.resamplingWindow; i++) {
                volumeBucket = input[i];
                for (int j = 0; j < harmonics.length; j++) {
                    harmonic = harmonics[j];
                    frequencyMultiplier = harmonic.getKey();
                    newFrequency = i * frequencyMultiplier;
                    x0 = (int) newFrequency;
                    x1 = x0 + 1;
                    xFraction = newFrequency - x0;
                    volumeMultiplier = harmonic.getValue();
                    if (x0 >= 0 && x0 < FFTEnvironment.resamplingWindow) {
                        harmonicsForThisVolumeSpectrum[x0] = harmonicsForThisVolumeSpectrum[x0].plus(volumeBucket.scale((1 - xFraction) * volumeMultiplier));
                    }
                    if (x1 >= 0 && x1 < FFTEnvironment.resamplingWindow) {
                        harmonicsForThisVolumeSpectrum[x1] = harmonicsForThisVolumeSpectrum[x1].plus(volumeBucket.scale(xFraction * volumeMultiplier));
                    }
                }
            }
            return harmonicsForThisVolumeSpectrum;
        }, "spectrum builder - calculate harmonics");
    }

    public PipeCallable<BoundedBuffer<Double, SimplePacket<Double>>, BoundedBuffer<Double, SimplePacket<Double>>> buildHarmonicSamplePipe(int resamplingWindow) {
        return inputBuffer ->
        {
            BoundedBuffer<Double, SimplePacket<Double>> outputBuffer = new SimpleBuffer<>(resamplingWindow * 2, "spectrum builder - harmonics from sample - output");

            new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {
                double[] history;
                int count = 0;

                {
                    history = new double[resamplingWindow];
                }

                @Override
                public void tick() {
                    double in = 0;
                    try {
                        in = input.consume().unwrap();

                        if (count >= history.length) {
                            count = 0;

                            double[] harmonizedSample = harmonizeSample(history);

                            for (Double amplitude : harmonizedSample) {
                                output.produce(new SimplePacket<>(amplitude));
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    history[count] = in;
                    count++;
                }

                private double[] harmonizeSample(double[] sample) {
                    double[] result = new double[sample.length];
                    Map.Entry<Double, Double> harmonic;
                    Double frequencyMultiplier;
                    Double volumeMultiplier;
                    int x0;
                    int x1;
                    double xFraction;

                    double harmonicSampleWindowSize;
                    double resampleIndex;
                    double value0;
                    double diff;
                    double finalValue;
                    double frontBackFraction;
                    for (int j = 0; j < harmonics.length; j++) {
                        harmonic = harmonics[j];
                        frequencyMultiplier = harmonic.getKey();
                        harmonicSampleWindowSize = (sample.length - 1) / frequencyMultiplier;
                        volumeMultiplier = harmonic.getValue();
                        for (int i = 0; i < sample.length; i++) {
                            frontBackFraction = ((double) i) / (sample.length - 1);
                            resampleIndex = (i % harmonicSampleWindowSize) * frequencyMultiplier;
                            {
                                x0 = (int) resampleIndex;
                                x1 = x0 + 1;
                                xFraction = resampleIndex - x0;
                                value0 = sample[x0];
                                if (xFraction > 0.) {
                                    diff = sample[x1] - value0;
                                    finalValue = value0 + xFraction * diff;
                                } else {
                                    finalValue = value0;
                                }
                                result[i] += finalValue * volumeMultiplier * (1. - frontBackFraction);
                            }

                            resampleIndex = sample.length - 1 - resampleIndex;
                            {
                                x0 = (int) resampleIndex;
                                x1 = x0 + 1;
                                xFraction = resampleIndex - x0;
                                value0 = sample[x0];
                                if (xFraction > 0.) {
                                    diff = sample[x1] - value0;
                                    finalValue = value0 + xFraction * diff;
                                } else {
                                    finalValue = value0;
                                }
                                result[sample.length - 1 - i] += finalValue * volumeMultiplier * (1. - frontBackFraction);
                            }
                        }
                    }

                    return result;
                }
            });
            return outputBuffer;
        };
    }
}