package main;

import component.*;
import frequency.Frequency;
import gui.GUI;
import spectrum.buckets.*;
import mixer.Mixer;
import spectrum.SpectrumWindow;
import spectrum.SpectrumBuilder;
import mixer.state.*;
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
import java.util.AbstractMap.SimpleImmutableEntry;

/*
The main components are:
GUI             - Takes in user input and note and harmonic spectra, sends out notes, renders
Mixer           - Takes in notes, sends out volumes
SoundEnvironment- Takes in volumes, plays sound
SpectrumBuilder - Takes in volumes, sends out note and harmonic spectra
Pianola         - Takes in note and harmonic spectra, and sends out notes

The component edges are:
GUI -> Mixer                        - sending notes
Mixer -> SoundEnvironment           - sending volumes
SoundEnvironment -> SpectrumBuilder - sending volumes
SpectrumBuilder -> GUI              - sending note and harmonic spectra
SpectrumBuilder -> Pianola          - sending note and harmonic spectra
Pianola -> Mixer                    - sending notes

The IO side effects are:
GUI                 - Takes in user input, renders
SoundEnvironment    - plays sound

The tickers for the components are:
Frame ticker    - notifies the GUI to render a new frame
Sample ticker   - notifies the Mixer to sample volumes
Pianola ticker  - notifies the Pianola to play new notes
*/
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

        int pianolaRate = 4;
        int pianolaLookahead = pianolaRate / 4;

        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        SpectrumWindow spectrumWindow = new SpectrumWindow(width, octaveRange);

        int capacity = 10;

        BoundedBuffer<Long> sampleCountBuffer = new BoundedBuffer<>(sampleLookahead, "sample ticker");
        new Ticker(sampleCountBuffer, new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate));
        BoundedBuffer<Frequency> newNoteBuffer = new BoundedBuffer<>(64, "new notes");
        BoundedBuffer<VolumeAmplitudeState> volumeBuffer = new BoundedBuffer<>(capacity, "sound environment - volume state");
        new Mixer(sampleCountBuffer, newNoteBuffer, volumeBuffer, sampleRate);

        BoundedBuffer<VolumeAmplitudeState> soundVolumeBuffer = new BoundedBuffer<>(capacity, "volume state to signal");
        BoundedBuffer<VolumeAmplitudeState> spectrumVolumeBuffer = new OverwritableBuffer<>(1, "sound - volume amplitude state out");
        new Broadcast<>(volumeBuffer, new HashSet<>(Arrays.asList(soundVolumeBuffer, spectrumVolumeBuffer)));

        try {
            new SoundEnvironment(soundVolumeBuffer, SAMPLE_SIZE_IN_BITS, sampleRate);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        BoundedBuffer<Pulse> frameTickBuffer = initializePulseTicker(frameRate, frameLookahead, "GUI ticker");
        BoundedBuffer<SimpleImmutableEntry<Buckets, Buckets>> spectrumBuffer = new BoundedBuffer<>(capacity, "spectrum buffer");
        new SpectrumBuilder(frameTickBuffer, spectrumVolumeBuffer, spectrumBuffer, width, spectrumWindow);

        BoundedBuffer<SimpleImmutableEntry<Buckets, Buckets>> guiSpectrumBuffer = new BoundedBuffer<>(capacity, "gui - notes buckets");
        BoundedBuffer<SimpleImmutableEntry<Buckets, Buckets>> pianolaSpectrumBuffer = new OverwritableBuffer<>(capacity);
        new Broadcast<>(spectrumBuffer, new HashSet<>(Arrays.asList(guiSpectrumBuffer, pianolaSpectrumBuffer)));

        new GUI(guiSpectrumBuffer, newNoteBuffer, spectrumWindow, width, inaudibleFrequencyMargin);

        BoundedBuffer<Pulse> pianolaTicker = initializePulseTicker(pianolaRate, pianolaLookahead, "Pianola ticker");
//        PianolaPattern pianolaPattern = new Sweep(this, 8, spectrumWindow.getCenterFrequency());
//        PianolaPattern pianolaPattern = new PatternPauser(8, new SweepToTarget(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, 5, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow), 5);
        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);
//        PianolaPattern pianolaPattern = new SimpleArpeggio(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer,3, spectrumWindow);
        new Pianola(pianolaTicker, pianolaSpectrumBuffer, newNoteBuffer, pianolaPattern, inaudibleFrequencyMargin);
        //todo create a complimentary pianola pattern which, at a certain rate, checks what notes are being played,
        //todo and plays harmonically complimentary notes near the notes being played. Use a higher frame rate preferably

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

    private static BoundedBuffer<Pulse> initializePulseTicker(int frameRate, int frameLookahead, String name) {
        BoundedBuffer<Pulse> outputBuffer = new BoundedBuffer<>(frameLookahead, name);
        BoundedBuffer<Long> tickBuffer = new BoundedBuffer<>(frameLookahead, name);
        new Ticker(tickBuffer, new TimeInSeconds(1).toNanoSeconds().divide(frameRate));
        new PipeComponent<>(tickBuffer, outputBuffer, input -> new Pulse());
        return outputBuffer;
    }

}
