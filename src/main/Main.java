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
import sound.SampleTicker;
import sound.SoundEnvironment;
import time.PerformanceTracker;
import time.Ticker;
import time.TimeInNanoSeconds;
import time.TimeInSeconds;

import java.util.Arrays;
import java.util.HashSet;

public class Main {

    public static void main(String[] args){
        new PerformanceTracker();
        PerformanceTracker.start();

        int SAMPLE_SIZE_IN_BITS = 8;
        int SAMPLE_RATE = 44100;

        int lookahead = SAMPLE_RATE / 20;

        BoundedBuffer<Double> amplitudeBuffer = new BoundedBuffer<>(lookahead);
        SoundEnvironment soundEnvironment = new SoundEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE, amplitudeBuffer);

        BoundedBuffer<Long> sampleCountBuffer = new BoundedBuffer<>(lookahead);
        BoundedBuffer<Frequency> newNoteBuffer = new BoundedBuffer<>(32);
        BoundedBuffer<VolumeState> volumeStateBuffer = new BoundedBuffer<>(1);
        new VolumeCalculator(sampleCountBuffer, newNoteBuffer, volumeStateBuffer, soundEnvironment.getSampleRate());

        BoundedBuffer<VolumeState> volumeStateBuffer2 = new BoundedBuffer<>(1);
        BoundedBuffer<VolumeState> volumeStateBuffer3 = new OverwritableBuffer<>(1);
        new Multiplexer<>(volumeStateBuffer, new HashSet<>(Arrays.asList(volumeStateBuffer2, volumeStateBuffer3)));
        new AmplitudeCalculator(volumeStateBuffer2, amplitudeBuffer, soundEnvironment.getSampleRate());

        BoundedBuffer<Buckets> guiHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> guiAveragedHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        new BucketsAverager(10, guiHarmonicsBucketsBuffer, guiAveragedHarmonicsBucketsBuffer);

        BoundedBuffer<Buckets> guiNotesBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Integer> cursorXBuffer = new OverwritableBuffer<>(1);
        GUI gui = new GUI(guiAveragedHarmonicsBucketsBuffer, guiNotesBucketsBuffer, cursorXBuffer);

        SpectrumWindow spectrumWindow = gui.spectrumWindow;

        gui.addMouseListener(new NoteClicker(newNoteBuffer, spectrumWindow));
        gui.addMouseMotionListener(new CursorMover(cursorXBuffer));

        HarmonicCalculator harmonicCalculator = new HarmonicCalculator();

        BoundedBuffer<TimeInNanoSeconds> frameEndTimeBuffer = new BoundedBuffer<>(1);

        BoundedBuffer<Buckets> inputHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> pianolaHarmonicsBucketsBuffer = new OverwritableBuffer<>(1);
        new Multiplexer<>(inputHarmonicsBucketsBuffer, new HashSet<>(Arrays.asList(guiHarmonicsBucketsBuffer, pianolaHarmonicsBucketsBuffer)));

        BoundedBuffer<Buckets> inputNotesBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> pianolaNotesBucketsBuffer = new OverwritableBuffer<>(1);
        new Multiplexer<>(inputNotesBucketsBuffer, new HashSet<>(Arrays.asList(guiNotesBucketsBuffer, pianolaNotesBucketsBuffer)));

        new SpectrumManager(spectrumWindow, harmonicCalculator, frameEndTimeBuffer, volumeStateBuffer3, inputNotesBucketsBuffer, inputHarmonicsBucketsBuffer);

        Ticker ticker = new Ticker(new TimeInSeconds(1).toNanoSeconds().divide(60));
        ticker.getTickObservable().add(new Observer<>() {
            private final OutputPort<TimeInNanoSeconds> frameEndTimeOutput = new OutputPort<>(frameEndTimeBuffer);

            @Override
            public void notify(Long event) {
                TimeInNanoSeconds frameEndTime = ticker.getFrameEndTime(TimeInNanoSeconds.now());
                try {
                    frameEndTimeOutput.produce(frameEndTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ticker.start();

        SampleTicker sampleTicker = new SampleTicker(new TimeInSeconds(1).toNanoSeconds().divide(soundEnvironment.getSampleRate().sampleRate), soundEnvironment.getSampleRate().sampleRate);
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

        new Pianola(spectrumWindow, new TimeInSeconds(1.).toNanoSeconds().divide(4), pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, newNoteBuffer);
        //todo create a complimentary pianola pattern which, at a certain rate, checks what notes are being played,

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

}
