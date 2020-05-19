package main;

import component.*;
import component.buffer.*;
import frequency.Frequency;
import gui.GUI;
import pianola.Pianola;
import pianola.patterns.PianolaPattern;
import pianola.patterns.SweepToTargetUpDown;
import sound.*;
import spectrum.SpectrumBuilder;
import spectrum.SpectrumWindow;
import time.Pulser;
import time.TimeInSeconds;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Main {

    static float gain = (1f / 100f);

    public static void main(String[] args) {
        TrafficAnalyzer trafficAnalyzer = new TrafficAnalyzer();

        int SAMPLE_RATE = 44100;
        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        int sampleLookahead = SAMPLE_RATE;
        int SAMPLE_SIZE_IN_BITS = 8;
        SoundEnvironment soundEnvironment = new SoundEnvironment(sampleRate, SAMPLE_SIZE_IN_BITS);
        FFTEnvironment fftEnvironment = new FFTEnvironment(sampleRate);
        SpectrumBuilder spectrumBuilder = new SpectrumBuilder(20, false);

        boolean microphoneOn = true;
        boolean IFFTSynthesis = true;
        boolean audioOutTonicOnly = false;
        boolean harmonicsFromSpectrumInsteadOfSample = false;
        boolean differenceOnly = false;

        int frameRate = 60 / 2;
        int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        double octaveRange = 3.;
        SpectrumWindow spectrumWindow = new SpectrumWindow(width, octaveRange);

        boolean pianolaOn = false;
        int inaudibleFrequencyMargin = (int) (width / octaveRange / 12 / 5);
        int pianolaRate = 1;
        int pianolaLookahead = pianolaRate / 4;


        Runnable build = new Main().build(soundEnvironment, sampleLookahead, sampleRate, microphoneOn, fftEnvironment, spectrumBuilder, IFFTSynthesis, audioOutTonicOnly, harmonicsFromSpectrumInsteadOfSample, differenceOnly, frameRate, spectrumWindow, pianolaOn, inaudibleFrequencyMargin, pianolaRate, pianolaLookahead);

        trafficAnalyzer.start();
        build.run();
    }

    public static Double[] toMagnitudeSpectrum(Complex[] input) {
        double[] magnitudes = CalculateFFT.getMagnitudes(input, input.length);
        return Arrays.stream(magnitudes).boxed().toArray(Double[]::new);
    }

    private Runnable build(SoundEnvironment soundEnvironment, int sampleLookahead, SampleRate sampleRate, boolean microphoneOn, FFTEnvironment fftEnvironment, SpectrumBuilder spectrumBuilder, boolean IFFTSynthesis, boolean audioOutTonicOnly, boolean harmonicsFromSpectrumInsteadOfSample, boolean differenceOnly, int frameRate, SpectrumWindow spectrumWindow, boolean pianolaOn, int inaudibleFrequencyMargin, int pianolaRate, int pianolaLookahead) {
        SimpleBuffer<Frequency, SimplePacket<Frequency>> newNoteBuffer = new SimpleBuffer<>(64, ("new notes"));

        BoundedBuffer<Complex[], SimplePacket<Complex[]>> volumesOfAudioIn;
        MethodOutputComponent<byte[]> audioIn;
        SimpleBuffer<Pulse, SimplePacket<Pulse>> secondTickerOutput;
        if(microphoneOn) {
            SimpleBuffer<byte[], SimplePacket<byte[]>> rawAudioInBuffer = new SimpleBuffer<>(sampleLookahead, "main - audio in buffer");
            audioIn = soundEnvironment.audioIn(rawAudioInBuffer);
            BoundedBuffer<Double, SimplePacket<Double>> audioInBuffer = soundEnvironment.prepareAudioFromMixer(rawAudioInBuffer);

            volumesOfAudioIn = audioInBuffer
                    .connectTo(fftEnvironment.buildAudioInPipe(spectrumWindow, sampleRate.sampleRate));

            secondTickerOutput = null;
        } else {
            secondTickerOutput = new SimpleBuffer<>(100, ("main - second ticker"));
            volumesOfAudioIn = secondTickerOutput.performMethod(input -> {
                Complex[] spectrum = new Complex[FFTEnvironment.resamplingWindow];
                for(int i = 0; i<FFTEnvironment.resamplingWindow; i++){
                    spectrum[i] = new Complex(0., 0.);
                }
                return spectrum;
            }, "main - second to empty spectrum");

            audioIn = null;
        }

        BoundedBuffer<Complex[], SimplePacket<Complex[]>> volumeBuffer = volumesOfAudioIn.performMethod(input -> {
            List<Frequency> call = Flusher.flush(newNoteBuffer).call(new Pulse());
            Complex[] output = new Complex[input.length];
            System.arraycopy(input, 0, output, 0, input.length);
            int[] ints = call.stream().mapToInt(freq -> (int) freq.getValue()).toArray();
            for(int i : ints){
                output[i] = output[i].plus(new Complex(5000., 0.));
            }
            return output;
        }, "main - add clicked and pianola notes to input");
        LinkedList<SimpleBuffer<Complex[], SimplePacket<Complex[]>>> volumeBroadcastAudio = new LinkedList<>(volumeBuffer.broadcast(2, "main note spectrum - broadcast"));

        SimpleBuffer<byte[], SimplePacket<byte[]>> rawAudioOutBuffer = new SimpleBuffer<>(sampleLookahead, "main - raw audio relay");
        BoundedBuffer<Double, SimplePacket<Double>> rawAmplitudeBuffer;
        if(audioOutTonicOnly){
            rawAmplitudeBuffer = soundEnvironment.synthesizeAudio(volumeBroadcastAudio.poll(), fftEnvironment, sampleRate, IFFTSynthesis, spectrumWindow);
        } else {
            if(harmonicsFromSpectrumInsteadOfSample) {
                if(!differenceOnly) {
                    rawAmplitudeBuffer = soundEnvironment.synthesizeAudio(spectrumBuilder.buildHarmonicSpectrumPipe(volumeBroadcastAudio.poll()), fftEnvironment, sampleRate, IFFTSynthesis, spectrumWindow);
                } else {
                    LinkedList<SimpleBuffer<Complex[], SimplePacket<Complex[]>>> volumeBroadcastForDifference = new LinkedList<>(volumeBroadcastAudio.poll().broadcast(2, "main - difference only volume broadcast"));
                    BoundedBuffer<Complex[], SimplePacket<Complex[]>> harmonics = spectrumBuilder.buildHarmonicSpectrumPipe(volumeBroadcastForDifference.poll());
                    BoundedBuffer<Complex[], SimplePacket<Complex[]>> finalVolumes = Pairer.pair(harmonics, volumeBroadcastForDifference.poll()).performMethod(input -> {
                        Complex[] output = new Complex[input.getKey().length];
                        for (int i = 0; i < input.getKey().length; i++) {
                            output[i] = input.getKey()[i].minus(input.getValue()[i]);
                        }
                        return output;
                    }, "main - calculate difference between harmonics and original signal");
                    rawAmplitudeBuffer = soundEnvironment.synthesizeAudio(finalVolumes, fftEnvironment, sampleRate, IFFTSynthesis, spectrumWindow);
                }
            } else {
                int resamplingWindow = sampleRate.sampleRate / 20;
                if(!differenceOnly) {
                    rawAmplitudeBuffer = soundEnvironment.synthesizeAudio(volumeBroadcastAudio.poll(), fftEnvironment, sampleRate, IFFTSynthesis, spectrumWindow).connectTo(spectrumBuilder.buildHarmonicSamplePipe(resamplingWindow));
                } else {
                    BoundedBuffer<Double, SimplePacket<Double>> synthesizedVolume = soundEnvironment.synthesizeAudio(volumeBroadcastAudio.poll(), fftEnvironment, sampleRate, IFFTSynthesis, spectrumWindow);
                    LinkedList<SimpleBuffer<Double, SimplePacket<Double>>> volumeBroadcastForDifference = new LinkedList<>(synthesizedVolume.broadcast(2, "main - difference only volume broadcast"));
                    BoundedBuffer<Double, SimplePacket<Double>> synthesizedHarmonics = volumeBroadcastForDifference.poll().connectTo(spectrumBuilder.buildHarmonicSamplePipe(resamplingWindow));
                    rawAmplitudeBuffer = Pairer.pair(synthesizedHarmonics, volumeBroadcastForDifference.poll().resize(resamplingWindow, "main - harmonic from sample resize")).performMethod(
                            input -> input.getKey()-input.getValue()
                    , "main - calculate difference between harmonics and original signal");
                }
            }
        }
        soundEnvironment.prepareAudioForMixer(rawAmplitudeBuffer).relayTo(rawAudioOutBuffer);
        MethodInputComponent<byte[], SimplePacket<byte[]>> audioOut = soundEnvironment.audioOut(rawAudioOutBuffer);

        SimpleBuffer<Pulse, SimplePacket<Pulse>> guiTickerOutput = new SimpleBuffer<>(1, ("main - dump GUI ticker overflow"));
        BoundedBuffer<Double[], SimplePacket<Double[]>> volumeAtGUISpeed = guiTickerOutput.connectTo(
                Memorizer.buildPipe(
                        volumeBroadcastAudio.poll()
                                .toOverwritable("main - volume spectrum overflow"), "main - volume spectrum memorizer"))
                .performMethod(Main::toMagnitudeSpectrum, "main - complex to magnitude")
//                .performMethod(input -> normalizeGain(input, spectrumWindow), "FFT - normalize magnitudes")
                .performMethod(Main::reduceNoise, "main - reduce noise")
                .performMethod(Main::normalize, "main - normalize");
        LinkedList<BoundedBuffer<Double[], SimplePacket<Double[]>>> volumeAtGUISpeedBroadcast = new LinkedList<>(volumeAtGUISpeed.broadcast(2));

        int pianolaOnValue;
        if(pianolaOn){
            pianolaOnValue = 1;
        } else {
            pianolaOnValue = 0;
        }
        LinkedList<BoundedBuffer<Double[], SimplePacket<Double[]>>> volumeSpectrumBroadcastForGUIAndPianola = new LinkedList<>(volumeAtGUISpeedBroadcast.poll().broadcast(1+pianolaOnValue, "main - volume spectrum for gui"));
        LinkedList<BoundedBuffer<Double[], SimplePacket<Double[]>>> harmonicSpectrumBroadcastForGUIAndPianola = new LinkedList<>(volumeAtGUISpeedBroadcast.poll().broadcast(1+pianolaOnValue, "main - harmonic spectrum for gui"));

        SimpleBuffer<java.util.List<Frequency>, ? extends Packet<java.util.List<Frequency>>> guiOutputBuffer = new SimpleBuffer<>(1, "gui output");
        guiOutputBuffer.connectTo(Separator.buildPipe("main - gui separate new notes")).relayTo(newNoteBuffer);
        new GUI<>(
                volumeSpectrumBroadcastForGUIAndPianola.poll(),
                harmonicSpectrumBroadcastForGUIAndPianola.poll(),
                guiOutputBuffer,
                spectrumWindow
        );

        SimpleBuffer<Pulse, SimplePacket<Pulse>> pianolaTickerOutput = new SimpleBuffer<>(1, ("main - dump pianola ticker overflow"));
        if(pianolaOn) {
            PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);
            SimpleBuffer<java.util.List<Frequency>, ? extends Packet<java.util.List<Frequency>>> pianolaOutputBuffer = new SimpleBuffer<>(1, "pianola output");
            pianolaOutputBuffer.connectTo(Separator.buildPipe("main - pianola separator")).relayTo(newNoteBuffer);
            new Pianola<>(
                    pianolaTickerOutput,
                    volumeSpectrumBroadcastForGUIAndPianola.poll(),
                    harmonicSpectrumBroadcastForGUIAndPianola.poll(),
                    pianolaOutputBuffer,
                    pianolaPattern,
                    inaudibleFrequencyMargin);
        }

        return () -> {
            new TickRunningStrategy(new Pulser(guiTickerOutput, new TimeInSeconds(1).toNanoSeconds().divide(frameRate)));

            if(pianolaOn) {
                new TickRunningStrategy(new Pulser(pianolaTickerOutput, new TimeInSeconds(1).toNanoSeconds().divide(pianolaRate)));
            }

            new TickRunningStrategy(audioOut);
            if(microphoneOn) {
                new TickRunningStrategy(audioIn);
            } else {
                new TickRunningStrategy(new Pulser(secondTickerOutput, new TimeInSeconds(1).toNanoSeconds()));
            }
        };
    }

    public static Double[] normalize(Double[] input) {
        Double[] output = new Double[input.length];
        double magnitude = Arrays.stream(input).reduce(0., Double::sum);
        if (magnitude != 0.) {
            for (int i = 0; i < input.length; i++) {
                output[i] = input[i] / magnitude;
            }
        } else {
            for (int i = 0; i < input.length; i++) {
                output[i] = 0.;
            }
        }
        return output;
    }

    public static Complex[] normalize(Complex[] input) {
        Complex[] output = new Complex[input.length];
        double magnitude = Arrays.stream(CalculateFFT.getMagnitudes(input, input.length)).reduce(0., Double::sum);
        if (magnitude != 0.) {
            for (int i = 0; i < input.length; i++) {
                output[i] = input[i].scale(1/magnitude);
            }
        } else {
            for (int i = 0; i < input.length; i++) {
                output[i] = new Complex(0., 0.);
            }
        }
        return output;
    }

    public Double[] normalizeGain(Double[] input) {
        Double[] output = new Double[input.length];
        double magnitude = Arrays.stream(input).reduce(0., Double::sum);
        if (magnitude != 0.) {
            for (int i = 0; i < input.length; i++) {
                output[i] = gain * input[i] / magnitude;
            }
        } else {
            for (int i = 0; i < input.length; i++) {
                output[i] = 0.;
            }
        }
        if (magnitude > 1.) {
            gain *= 0.9999f;
        } else {
            gain /= 0.9999f;
        }
//        SoundEnvironment.volCtrl.setValue(gain);
        return output;
    }

    public static Double[] reduceNoise(Double[] input) {
//        //                    either reduce below median or below average. Could also reduce below 75th percentile or something.
////                    Double noiseMeasure = Arrays.stream(input).sorted().collect(Collectors.toList()).get((int) (0.75 * spectrumWindow.width));
//        Double noiseMeasure = Arrays.stream(input).reduce(0., Double::sum) / spectrumWindow.width;
//        Double[] output = new Double[spectrumWindow.width];
//        for (int i = 0; i < spectrumWindow.width; i++) {
//            if (input[i] <= noiseMeasure) {
//                output[i] = 0.;
//            } else {
//                output[i] = input[i];
//            }
//        }
//        return output;

        Double noiseMeasure = Arrays.stream(input).reduce(0., Double::sum) / input.length;
//        Double noiseMeasure = Arrays.stream(input).min(Double::compare).orElse(0.);
        Double[] output = new Double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = Math.max(0., input[i] - noiseMeasure);
        }
        return output;
    }

    private static Complex[] reduceNoise(Complex[] input) {
        double[] magnitudes = CalculateFFT.getMagnitudes(input, input.length);
        Double noiseMeasure = Arrays.stream(magnitudes).reduce(0., Double::sum) / input.length;
//        Double noiseMeasure = Arrays.stream(input).min(Double::compare).orElse(0.);
        Complex[] output = new Complex[input.length];
        for (int i = 0; i < input.length; i++) {
            double oldMagnitude = magnitudes[i];
            double newMagnitude = Math.max(0., oldMagnitude - noiseMeasure);
            output[i] = input[i].scale(newMagnitude/oldMagnitude);
        }
        return output;
    }

    private static void playTestTone(SimpleBuffer<Frequency, SimplePacket<Frequency>> newNoteBuffer, SpectrumWindow spectrumWindow) {
        OutputPort<Frequency, SimplePacket<Frequency>> frequencyOutputPort = new OutputPort<>(newNoteBuffer);
        Frequency centerFrequency = spectrumWindow.getCenterFrequency();
        try {
            frequencyOutputPort.produce(new SimplePacket<>(centerFrequency));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
