package main;

import frequency.Frequency;
import gui.CursorMover;
import gui.GUI;
import gui.NoteClicker;
import gui.buckets.Buckets;
import gui.buckets.BucketsAverager;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumManager;
import harmonics.HarmonicCalculator;
import notes.state.AmplitudeCalculator;
import notes.state.VolumeCalculator;
import notes.state.VolumeState;
import pianola.Pianola;
import sound.SampleRate;
import sound.SoundEnvironment;
import time.PerformanceTracker;
import time.Ticker;
import time.TimeInNanoSeconds;
import time.TimeInSeconds;

import javax.sound.sampled.LineUnavailableException;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;

public class Main {

    public static void main(String[] args){
        new PerformanceTracker();
        PerformanceTracker.start();

        int SAMPLE_SIZE_IN_BITS = 8;
        int SAMPLE_RATE = 44100;

        int lookahead = SAMPLE_RATE / 4;

        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);

        BoundedBuffer<Long> sampleCountBuffer = initializeSampleTicker(lookahead, sampleRate);

        BoundedBuffer<TimeInNanoSeconds> frameEndTimeBuffer = initializeGUITicker();

        BoundedBuffer<Frequency> newNoteBuffer = new BoundedBuffer<>(32);
        BoundedBuffer<VolumeState> volumeStateBuffer = new BoundedBuffer<>(1);
        new VolumeCalculator(sampleCountBuffer, newNoteBuffer, volumeStateBuffer, sampleRate);

        BoundedBuffer<VolumeState> volumeStateBuffer2 = new BoundedBuffer<>(1);
        BoundedBuffer<VolumeState> volumeStateBuffer3 = new OverwritableBuffer<>(1);
        new Multiplexer<>(volumeStateBuffer, new HashSet<>(Arrays.asList(volumeStateBuffer2, volumeStateBuffer3)));
        BoundedBuffer<Double> amplitudeBuffer = new BoundedBuffer<>(lookahead);
        new AmplitudeCalculator(volumeStateBuffer2, amplitudeBuffer, sampleRate);

        initializeSoundEnvironment(SAMPLE_SIZE_IN_BITS, sampleRate, amplitudeBuffer);

        HarmonicCalculator harmonicCalculator = new HarmonicCalculator(100);
        BoundedBuffer<Buckets> inputNotesBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> inputHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        SpectrumWindow spectrumWindow = new SpectrumWindow(width);
        new SpectrumManager(spectrumWindow, harmonicCalculator, frameEndTimeBuffer, volumeStateBuffer3, inputNotesBucketsBuffer, inputHarmonicsBucketsBuffer);

        BoundedBuffer<Buckets> guiNotesBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> pianolaNotesBucketsBuffer = new OverwritableBuffer<>(1);
        new Multiplexer<>(inputNotesBucketsBuffer, new HashSet<>(Arrays.asList(guiNotesBucketsBuffer, pianolaNotesBucketsBuffer)));
        BoundedBuffer<Buckets> guiHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> pianolaHarmonicsBucketsBuffer = new OverwritableBuffer<>(1);
        new Multiplexer<>(inputHarmonicsBucketsBuffer, new HashSet<>(Arrays.asList(guiHarmonicsBucketsBuffer, pianolaHarmonicsBucketsBuffer)));
        BoundedBuffer<Buckets> guiAveragedHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        new BucketsAverager(10, guiHarmonicsBucketsBuffer, guiAveragedHarmonicsBucketsBuffer);
        BoundedBuffer<Integer> cursorXBuffer = new OverwritableBuffer<>(1);
        GUI gui = new GUI(guiAveragedHarmonicsBucketsBuffer, guiNotesBucketsBuffer, cursorXBuffer, width);

        gui.addMouseListener(new NoteClicker(newNoteBuffer, spectrumWindow));
        gui.addMouseMotionListener(new CursorMover(cursorXBuffer));

        new Pianola(spectrumWindow, new TimeInSeconds(1.).toNanoSeconds().divide(4), pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, newNoteBuffer);
        //todo create a complimentary pianola pattern which, at a certain rate, checks what notes are being played,

        playTestTone(newNoteBuffer, spectrumWindow);
    }

    private static void playTestTone(BoundedBuffer<Frequency> newNoteBuffer, SpectrumWindow spectrumWindow) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            new OutputPort<>(newNoteBuffer).produce(spectrumWindow.getCenterFrequency());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void initializeSoundEnvironment(int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate, BoundedBuffer<Double> amplitudeBuffer) {
        try {
            new SoundEnvironment(amplitudeBuffer, SAMPLE_SIZE_IN_BITS, sampleRate);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private static BoundedBuffer<TimeInNanoSeconds> initializeGUITicker() {
        BoundedBuffer<TimeInNanoSeconds> frameEndTimeBuffer = new BoundedBuffer<>(1);
        Ticker frameTicker = new Ticker(new TimeInSeconds(1).toNanoSeconds().divide(60));
        frameTicker.getTickObservable().add(new Observer<>() {
            private final OutputPort<TimeInNanoSeconds> frameEndTimeOutput = new OutputPort<>(frameEndTimeBuffer);

            @Override
            public void notify(Long event) {
                TimeInNanoSeconds frameEndTime = frameTicker.getFrameEndTime(TimeInNanoSeconds.now());
                try {
                    frameEndTimeOutput.produce(frameEndTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        frameTicker.start();
        return frameEndTimeBuffer;
    }

    private static BoundedBuffer<Long> initializeSampleTicker(int lookahead, SampleRate sampleRate) {
        BoundedBuffer<Long> sampleCountBuffer = new BoundedBuffer<>(lookahead);
        Ticker sampleTicker = new Ticker(new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate));
        sampleTicker.getTickObservable().add(new Observer<>() {
            private final OutputPort<Long> longOutputPort = new OutputPort<>(sampleCountBuffer);

            @Override
            public void notify(Long event) {
                try {
                    longOutputPort.produce(event);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        sampleTicker.start();
        return sampleCountBuffer;
    }

}
