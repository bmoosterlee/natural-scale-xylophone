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
    private static int count = 0;
    private static int capacity = 100000;

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

        int pianolaRate = 4;
        int pianolaLookahead = pianolaRate / 4;

        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        SpectrumWindow spectrumWindow = new SpectrumWindow(width, octaveRange);

        BoundedBuffer<Frequency> newNoteBuffer = new BoundedBuffer<>(64, "new notes");
        BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer3 = new BoundedBuffer<>(capacity, String.valueOf(count));
        count++;
        initalizeSoundPipeline(sampleLookahead, SAMPLE_SIZE_IN_BITS, sampleRate, newNoteBuffer, volumeAmplitudeStateBuffer3);

        BoundedBuffer<Buckets> inputNotesBucketsBuffer = new BoundedBuffer<>(capacity, "spectrum - note buckets");
        BoundedBuffer<Buckets> timeAveragedHarmonicsBucketsBuffer = new BoundedBuffer<>(capacity, "spectrum - harmonics buckets");
        initializeSpectrumPipeline(frameRate, frameLookahead, width, spectrumWindow, volumeAmplitudeStateBuffer3, inputNotesBucketsBuffer, timeAveragedHarmonicsBucketsBuffer);

        BoundedBuffer<Buckets> guiNotesBucketsBuffer = new BoundedBuffer<>(capacity, "gui - notes buckets");
        BoundedBuffer<Buckets> pianolaNotesBucketsBuffer = new OverwritableBuffer<>(capacity);
        new Broadcast<>(inputNotesBucketsBuffer, new HashSet<>(Arrays.asList(guiNotesBucketsBuffer, pianolaNotesBucketsBuffer)));
        BoundedBuffer<Buckets> guiHarmonicsBucketsBuffer = new BoundedBuffer<>(capacity, "gui - harmonics buckets");
        BoundedBuffer<Buckets> pianolaHarmonicsBucketsBuffer = new OverwritableBuffer<>(capacity);
        new Broadcast<>(timeAveragedHarmonicsBucketsBuffer, new HashSet<>(Arrays.asList(guiHarmonicsBucketsBuffer, pianolaHarmonicsBucketsBuffer)));
        initializeGUIPipeline(width, inaudibleFrequencyMargin, spectrumWindow, newNoteBuffer, guiNotesBucketsBuffer, guiHarmonicsBucketsBuffer);
        initializePianolaPipeline(pianolaRate, pianolaLookahead, inaudibleFrequencyMargin, spectrumWindow, newNoteBuffer, pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer);

        playTestTone(newNoteBuffer, spectrumWindow);
    }

    private static void initializeSpectrumPipeline(int frameRate, int frameLookahead, int width, SpectrumWindow spectrumWindow, BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer3, BoundedBuffer<Buckets> inputNotesBucketsBuffer, BoundedBuffer<Buckets> timeAveragedHarmonicsBucketsBuffer) {
        BoundedBuffer<VolumeState> volumeStateBuffer = new BoundedBuffer<>(capacity, String.valueOf(count));
        count++;
        new VolumeAmplitudeToVolumeFilter(volumeAmplitudeStateBuffer3, volumeStateBuffer);

        BoundedBuffer<VolumeState> volumeStateBuffer2 = new OverwritableBuffer<>(capacity);
        BoundedBuffer<VolumeState> volumeStateBuffer3 = new OverwritableBuffer<>(capacity);
        new Broadcast<>(volumeStateBuffer, new HashSet<>(Arrays.asList(volumeStateBuffer2, volumeStateBuffer3)));

        BoundedBuffer<Pulse> frameTickBuffer = initializePulseTicker(frameRate, frameLookahead, "GUI ticker");

        BoundedBuffer<Pulse> frameTickBuffer1 = new BoundedBuffer<>(capacity, String.valueOf(count));
        count++;
        BoundedBuffer<Pulse> frameTickBuffer2 = new BoundedBuffer<>(capacity, String.valueOf(count));
        count++;
        BoundedBuffer<Pulse> frameTickBuffer3 = new BoundedBuffer<>(capacity, String.valueOf(count));
        count++;
        new Broadcast<>(frameTickBuffer, new HashSet<>(Arrays.asList(frameTickBuffer1, frameTickBuffer2, frameTickBuffer3)));
        new VolumeStateToBuckets(spectrumWindow, frameTickBuffer1, volumeStateBuffer2, inputNotesBucketsBuffer);

        BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> harmonicsBuffer = new BoundedBuffer<>(capacity, String.valueOf(count));
        count++;
        new HarmonicCalculator(100, frameTickBuffer2, volumeStateBuffer3, harmonicsBuffer);

        Map<Integer, BoundedBuffer<AtomicBucket>> harmonicsMap = new HashMap<>();
        for(Integer i = 0; i< width; i++){
            harmonicsMap.put(i, new BoundedBuffer<>(1000, "harmonics bucket"));
        }
        new SpectrumManager(spectrumWindow, harmonicsBuffer, harmonicsMap);

        BoundedBuffer<Buckets> inputHarmonicsBucketsBuffer = new BoundedBuffer<>(capacity, String.valueOf(count));
        count++;
        //todo find all uses of history component and check whether we can eliminate the conversion from buffers to buckets.
        new BuffersToBuckets(harmonicsMap, frameTickBuffer3, inputHarmonicsBucketsBuffer);
        new BucketHistoryComponent(200, inputHarmonicsBucketsBuffer, timeAveragedHarmonicsBucketsBuffer);
    }

    private static void initializeGUIPipeline(int width, int inaudibleFrequencyMargin, SpectrumWindow spectrumWindow, BoundedBuffer<Frequency> newNoteBuffer, BoundedBuffer<Buckets> guiNotesBucketsBuffer, BoundedBuffer<Buckets> guiHarmonicsBucketsBuffer) {
        BoundedBuffer<Buckets> guiAveragedHarmonicsBucketsBuffer = new BoundedBuffer<>(capacity, String.valueOf(count));
        count++;
        new BucketsAverager(inaudibleFrequencyMargin, guiHarmonicsBucketsBuffer, guiAveragedHarmonicsBucketsBuffer);
        BoundedBuffer<Integer> cursorXBuffer = new OverwritableBuffer<>(capacity);
        GUI gui = new GUI(guiAveragedHarmonicsBucketsBuffer, guiNotesBucketsBuffer, cursorXBuffer, width);

        gui.addMouseListener(new NoteClicker(newNoteBuffer, spectrumWindow));
        gui.addMouseMotionListener(new CursorMover(cursorXBuffer));
    }

    private static void initializePianolaPipeline(int pianolaRate, int pianolaLookahead, int inaudibleFrequencyMargin, SpectrumWindow spectrumWindow, BoundedBuffer<Frequency> newNoteBuffer, BoundedBuffer<Buckets> pianolaNotesBucketsBuffer, BoundedBuffer<Buckets> pianolaHarmonicsBucketsBuffer) {
        BoundedBuffer<Pulse> pianolaTicker = initializePulseTicker(pianolaRate, pianolaLookahead, "Pianola ticker");

//        PianolaPattern pianolaPattern = new Sweep(this, 8, spectrumWindow.getCenterFrequency());
//        PianolaPattern pianolaPattern = new PatternPauser(8, new SweepToTarget(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, 5, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow), 5);
        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);
//        PianolaPattern pianolaPattern = new SimpleArpeggio(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer,3, spectrumWindow);
        new Pianola(pianolaPattern, pianolaTicker, newNoteBuffer, pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, inaudibleFrequencyMargin);
        //todo create a complimentary pianola pattern which, at a certain rate, checks what notes are being played,
    }

    private static void initalizeSoundPipeline(int sampleLookahead, int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate, BoundedBuffer<Frequency> newNoteBuffer, BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer3) {
        BoundedBuffer<Long> sampleCountBuffer = initializeSampleTicker(sampleRate, sampleLookahead, "Sample ticker");
        BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer = new BoundedBuffer<>(capacity, "sound environment - volume state");
        BoundedBuffer<TimestampedFrequencies> timeStampedNewNotesBuffer = new BoundedBuffer<>(capacity, "note timestamper");
        new NoteTimestamper(sampleCountBuffer, newNoteBuffer, timeStampedNewNotesBuffer);
        new VolumeAmplitudeCalculator(timeStampedNewNotesBuffer, volumeAmplitudeStateBuffer, sampleRate);

        BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer2 = new BoundedBuffer<>(capacity, "volume state to signal");
        new Broadcast<>(volumeAmplitudeStateBuffer, new HashSet<>(Arrays.asList(volumeAmplitudeStateBuffer2, volumeAmplitudeStateBuffer3)));
        BoundedBuffer<Double> amplitudeBuffer = new BoundedBuffer<>(capacity, "sound environment - signal");
        new VolumeAmplitudeStateToSignal(volumeAmplitudeStateBuffer2, amplitudeBuffer);

        initializeSoundEnvironment(SAMPLE_SIZE_IN_BITS, sampleRate, amplitudeBuffer);
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

    private static BoundedBuffer<Pulse> initializePulseTicker(int frameRate, int frameLookahead, String name) {
        BoundedBuffer<Pulse> outputBuffer = new BoundedBuffer<>(frameLookahead, name);
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

    private static BoundedBuffer<Long> initializeSampleTicker(SampleRate sampleRate, int sampleLookahead, String name) {
        BoundedBuffer<Long> sampleCountBuffer = new BoundedBuffer<>(sampleLookahead, name);
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
