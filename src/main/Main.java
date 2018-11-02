package main;

import frequency.Frequency;
import gui.CursorMover;
import gui.GUI;
import gui.NoteClicker;
import gui.buckets.*;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.VolumeStateToBuckets;
import gui.spectrum.state.SpectrumManager;
import harmonics.Harmonic;
import harmonics.HarmonicCalculator;
import notes.state.*;
import pianola.Pianola;
import pianola.patterns.PianolaPattern;
import pianola.patterns.SweepToTargetUpDown;
import sound.SampleRate;
import sound.SoundEnvironment;
import time.PerformanceTracker;
import time.Ticker;
import time.TimeInSeconds;

import javax.sound.sampled.LineUnavailableException;
import java.awt.*;
import java.util.*;

class Main {

    public static void main(String[] args){
        new PerformanceTracker();
        PerformanceTracker.start();

        int SAMPLE_RATE = 44100/4;
        int sampleLookahead = SAMPLE_RATE / 4;
        int SAMPLE_SIZE_IN_BITS = 8;

        int frameRate = 30;
        int frameLookahead = frameRate / 4;
        int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();

        double octaveRange = 3.;
        int inaudibleFrequencyMargin = (int) (width/octaveRange/12/5);

        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        BoundedBuffer<Long> sampleCountBuffer = initializeSampleTicker(sampleRate, sampleLookahead);
        BoundedBuffer<Frequency> newNoteBuffer = new BoundedBuffer<>(32);
        BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer = new BoundedBuffer<>(1);
        new VolumeAmplitudeCalculator(sampleCountBuffer, newNoteBuffer, volumeAmplitudeStateBuffer, sampleRate);

        BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer2 = new BoundedBuffer<>(1);
        BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer3 = new BoundedBuffer<>(1);
        new Broadcast<>(volumeAmplitudeStateBuffer, new HashSet<>(Arrays.asList(volumeAmplitudeStateBuffer2, volumeAmplitudeStateBuffer3)));
        BoundedBuffer<Double> amplitudeBuffer = new BoundedBuffer<>(1);
        new VolumeAmplitudeStateToSignal(volumeAmplitudeStateBuffer2, amplitudeBuffer);

        initializeSoundEnvironment(SAMPLE_SIZE_IN_BITS, sampleRate, amplitudeBuffer);

        BoundedBuffer<VolumeState> volumeStateBuffer = new BoundedBuffer<>(1);
        new VolumeAmplitudeToVolumeFilter(volumeAmplitudeStateBuffer3, volumeStateBuffer);

        BoundedBuffer<VolumeState> volumeStateBuffer2 = new OverwritableBuffer<>(1);
        BoundedBuffer<VolumeState> volumeStateBuffer3 = new OverwritableBuffer<>(1);
        new Broadcast<>(volumeStateBuffer, new HashSet<>(Arrays.asList(volumeStateBuffer2, volumeStateBuffer3)));

        BoundedBuffer<Pulse> frameTickBuffer = initializePulseTicker(frameRate, frameLookahead);

        SpectrumWindow spectrumWindow = new SpectrumWindow(width, octaveRange);
        BoundedBuffer<Pulse> frameTickBuffer1 = new BoundedBuffer<>(1);
        BoundedBuffer<Pulse> frameTickBuffer2 = new BoundedBuffer<>(1);
        BoundedBuffer<Pulse> frameTickBuffer3 = new BoundedBuffer<>(1);
        new Broadcast<>(frameTickBuffer, new HashSet<>(Arrays.asList(frameTickBuffer1, frameTickBuffer2, frameTickBuffer3)));
        BoundedBuffer<Buckets> inputNotesBucketsBuffer = new BoundedBuffer<>(1);
        new VolumeStateToBuckets(spectrumWindow, frameTickBuffer1, volumeStateBuffer3, inputNotesBucketsBuffer);

        BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> harmonicsBuffer = new BoundedBuffer<>(1);
        new HarmonicCalculator(100, frameTickBuffer2, volumeStateBuffer2, harmonicsBuffer);

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
        new Broadcast<>(timeAveragedHarmonicsBucketsBuffer, new HashSet<>(Arrays.asList(guiHarmonicsBucketsBuffer, pianolaHarmonicsBucketsBuffer)));

        BoundedBuffer<Buckets> guiAveragedHarmonicsBucketsBuffer = new BoundedBuffer<>(1);
        new BucketsAverager(inaudibleFrequencyMargin, guiHarmonicsBucketsBuffer, guiAveragedHarmonicsBucketsBuffer);
        BoundedBuffer<Buckets> guiNotesBucketsBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<Buckets> pianolaNotesBucketsBuffer = new OverwritableBuffer<>(1);
        new Broadcast<>(inputNotesBucketsBuffer, new HashSet<>(Arrays.asList(guiNotesBucketsBuffer, pianolaNotesBucketsBuffer)));
        BoundedBuffer<Integer> cursorXBuffer = new OverwritableBuffer<>(1);
        GUI gui = new GUI(guiAveragedHarmonicsBucketsBuffer, guiNotesBucketsBuffer, cursorXBuffer, width);

        gui.addMouseListener(new NoteClicker(newNoteBuffer, spectrumWindow));
        gui.addMouseMotionListener(new CursorMover(cursorXBuffer));

        int pianolaRate = 4;
        int pianolaLookahead = pianolaRate/4;

        BoundedBuffer<Pulse> pianolaTicker = initializePulseTicker(pianolaRate, pianolaLookahead);

//        PianolaPattern pianolaPattern = new Sweep(this, 8, spectrumWindow.getCenterFrequency());
//        PianolaPattern pianolaPattern = new PatternPauser(8, new SweepToTarget(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, 5, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow), 5);
        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);
//        PianolaPattern pianolaPattern = new SimpleArpeggio(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer,3, spectrumWindow);
        new Pianola(pianolaPattern, pianolaTicker, newNoteBuffer, pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, inaudibleFrequencyMargin);
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

    private static BoundedBuffer<Pulse> initializePulseTicker(int frameRate, int frameLookahead) {
        BoundedBuffer<Pulse> outputBuffer = new BoundedBuffer<>(frameLookahead);
        Ticker frameTicker = new Ticker(new TimeInSeconds(1).toNanoSeconds().divide(frameRate));
        frameTicker.getTickObservable().add(new Observer<>() {
            private final OutputPort<Pulse> frameEndTimeOutput = new OutputPort<>(outputBuffer);
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
        return outputBuffer;
    }

    private static BoundedBuffer<Long> initializeSampleTicker(SampleRate sampleRate, int sampleLookahead) {
        BoundedBuffer<Long> sampleCountBuffer = new BoundedBuffer<>(sampleLookahead);
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
