package main;

import frequency.Frequency;
import gui.CursorMover;
import gui.GUI;
import gui.NoteClicker;
import gui.buckets.*;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.BucketBuilder;
import gui.spectrum.state.SpectrumManager;
import harmonics.Harmonic;
import harmonics.HarmonicCalculator;
import notes.state.AmplitudeCalculator;
import notes.state.VolumeCalculator;
import notes.state.VolumeState;
import pianola.Pianola;
import sound.SampleRate;
import sound.SoundEnvironment;
import time.PerformanceTracker;
import time.Ticker;
import time.TimeInSeconds;

import javax.sound.sampled.LineUnavailableException;
import java.awt.*;
import java.util.*;

public class Main {

    public static void main(String[] args){
        new PerformanceTracker();
        PerformanceTracker.start();

        int SAMPLE_RATE = 44100/4;
        int sampleLookahead = SAMPLE_RATE / 4;
        int SAMPLE_SIZE_IN_BITS = 8;

        int frameRate = 30;
        int frameLookahead = frameRate / 4;
        int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();


        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        BoundedBuffer<Long> sampleCountBuffer = initializeSampleTicker(sampleLookahead, sampleRate);

        BoundedBuffer<Frequency> newNoteBuffer = new BoundedBuffer<>(32);
        BoundedBuffer<VolumeState> volumeStateBuffer = new BoundedBuffer<>(1);
        new VolumeCalculator(sampleCountBuffer, newNoteBuffer, volumeStateBuffer, sampleRate);

        BoundedBuffer<VolumeState> volumeStateBuffer2 = new BoundedBuffer<>(1);
        BoundedBuffer<VolumeState> volumeStateBuffer3 = new OverwritableBuffer<>(1);
        BoundedBuffer<VolumeState> volumeStateBuffer4 = new OverwritableBuffer<>(1);
        new Multiplexer<>(volumeStateBuffer, new HashSet<>(Arrays.asList(volumeStateBuffer2, volumeStateBuffer3, volumeStateBuffer4)));
        BoundedBuffer<Double> amplitudeBuffer = new BoundedBuffer<>(sampleLookahead);
        new AmplitudeCalculator(volumeStateBuffer2, amplitudeBuffer, sampleRate);

        initializeSoundEnvironment(SAMPLE_SIZE_IN_BITS, sampleRate, amplitudeBuffer);

        BoundedBuffer<Pulse> frameTickBuffer = initializeGUITicker(frameLookahead, frameRate);

        SpectrumWindow spectrumWindow = new SpectrumWindow(width);
        BoundedBuffer<Pulse> frameTickBuffer1 = new BoundedBuffer<>(1);
        BoundedBuffer<Pulse> frameTickBuffer2 = new BoundedBuffer<>(1);
        BoundedBuffer<Pulse> frameTickBuffer3 = new BoundedBuffer<>(1);
        new Multiplexer<>(frameTickBuffer, new HashSet<>(Arrays.asList(frameTickBuffer1, frameTickBuffer2, frameTickBuffer3)));
        BoundedBuffer<Buckets> inputNotesBucketsBuffer = new BoundedBuffer<>(1);
        new BucketBuilder(spectrumWindow, frameTickBuffer1, volumeStateBuffer4, inputNotesBucketsBuffer);

        BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> harmonicsBuffer = new BoundedBuffer<>(1);
        new HarmonicCalculator(100, frameTickBuffer2, volumeStateBuffer3, harmonicsBuffer);

        Map<Integer, BoundedBuffer<AtomicBucket>> harmonicsMap = new HashMap<>();
        for(Integer i = 0; i<width; i++){
            harmonicsMap.put(i, new BoundedBuffer<>(1000));
        }
        new SpectrumManager(spectrumWindow, harmonicsBuffer, harmonicsMap);

        BoundedBuffer<Buckets> inputHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        new BuffersToBuckets(harmonicsMap, frameTickBuffer3, inputHarmonicsBucketsBuffer);
        BoundedBuffer<Buckets> timeAveragedHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        new BucketHistoryComponent(200, inputHarmonicsBucketsBuffer, timeAveragedHarmonicsBucketsBuffer);
        BoundedBuffer<Buckets> guiHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> pianolaHarmonicsBucketsBuffer = new OverwritableBuffer<>(1);
        new Multiplexer<>(timeAveragedHarmonicsBucketsBuffer, new HashSet<>(Arrays.asList(guiHarmonicsBucketsBuffer, pianolaHarmonicsBucketsBuffer)));

        BoundedBuffer<Buckets> guiAveragedHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        new BucketsAverager(10, guiHarmonicsBucketsBuffer, guiAveragedHarmonicsBucketsBuffer);
        BoundedBuffer<Buckets> guiNotesBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> pianolaNotesBucketsBuffer = new OverwritableBuffer<>(1);
        new Multiplexer<>(inputNotesBucketsBuffer, new HashSet<>(Arrays.asList(guiNotesBucketsBuffer, pianolaNotesBucketsBuffer)));
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

    private static BoundedBuffer<Pulse> initializeGUITicker(int lookahead, int franeRate) {
        BoundedBuffer<Pulse> frameEndTimeBuffer = new BoundedBuffer<>(lookahead);
        Ticker frameTicker = new Ticker(new TimeInSeconds(1).toNanoSeconds().divide(franeRate));
        frameTicker.getTickObservable().add(new Observer<>() {
            private final OutputPort<Pulse> frameEndTimeOutput = new OutputPort<>(frameEndTimeBuffer);
            private final Pulse pulse = new Pulse();

            @Override
            public void notify(Long event) {
                try {
                    frameEndTimeOutput.produce(pulse);
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
