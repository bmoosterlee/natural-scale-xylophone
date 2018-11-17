package main;

import component.Counter;
import component.Unzipper;
import component.buffer.OutputPort;
import component.buffer.RunningOutputComponent;
import component.buffer.SimpleBuffer;
import frequency.Frequency;
import gui.GUI;
import mixer.Mixer;
import mixer.state.VolumeAmplitudeState;
import pianola.Pianola;
import pianola.patterns.PianolaPattern;
import pianola.patterns.SweepToTargetUpDown;
import sound.SampleRate;
import sound.SoundEnvironment;
import spectrum.SpectrumBuilder;
import spectrum.SpectrumWindow;
import spectrum.buckets.Buckets;
import time.PerformanceTracker;
import time.Ticker;
import time.TimeInSeconds;

import java.awt.*;
import java.util.LinkedList;

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

        SimpleBuffer<Frequency> newNoteBuffer = new SimpleBuffer<>(64, "new notes");
        LinkedList<SimpleBuffer<VolumeAmplitudeState>> volumeBroadcast =
            new LinkedList<>(
                RunningOutputComponent.buildOutputBuffer(
                    Ticker.build(new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate)),
                        sampleLookahead,
                        "sample ticker - output")
                .performMethod(Counter.build(), "count samples")
                .connectTo(Mixer.buildPipe(newNoteBuffer, sampleRate))
                .broadcast(2, "main volume - broadcast"));

        volumeBroadcast.poll()
        .connectTo(SoundEnvironment.buildPipe(SAMPLE_SIZE_IN_BITS, sampleRate));

        SimpleBuffer<Buckets> noteSpectrumBuffer = new SimpleBuffer<>(1, "note spectrum output");
        SimpleBuffer<Buckets> harmonicSpectrumBuffer = new SimpleBuffer<>(1, "note spectrum output");
        new SpectrumBuilder(
            RunningOutputComponent.buildOutputBuffer(
                Ticker.build(new TimeInSeconds(1).toNanoSeconds().divide(frameRate)), frameLookahead, "GUI ticker"),
            volumeBroadcast.poll()
                .toOverwritable(),
            noteSpectrumBuffer,
            harmonicSpectrumBuffer,
            spectrumWindow,
            width);

        LinkedList<SimpleBuffer<Buckets>> noteSpectrumBroadcast = new LinkedList<>(noteSpectrumBuffer.broadcast(2, "main note spectrum - broadcast"));
        LinkedList<SimpleBuffer<Buckets>> harmonicSpectrumBroadcast = new LinkedList<>(harmonicSpectrumBuffer.broadcast(2, "main harmonic spectrum - broadcast"));

        SimpleBuffer<java.util.List<Frequency>> guiOutputBuffer = new SimpleBuffer<>(1, "gui output");
        Unzipper.unzip(guiOutputBuffer).relayTo(newNoteBuffer);
        new GUI(
            noteSpectrumBroadcast.poll(),
            harmonicSpectrumBroadcast.poll(),
            guiOutputBuffer,
            spectrumWindow,
            width,
            inaudibleFrequencyMargin);

//        PianolaPattern pianolaPattern = new Sweep(this, 8, spectrumWindow.getCenterFrequency());
//        PianolaPattern pianolaPattern = new PatternPauser(8, new SweepToTarget(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, 5, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow), 5);
        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);
//        PianolaPattern pianolaPattern = new SimpleArpeggio(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer,3, spectrumWindow);


        SimpleBuffer<java.util.List<Frequency>> pianolaOutputBuffer = new SimpleBuffer<>(1, "gui output");
        Unzipper.unzip(pianolaOutputBuffer).relayTo(newNoteBuffer);

        new Pianola(
            RunningOutputComponent.buildOutputBuffer(
                Ticker.build(new TimeInSeconds(1).toNanoSeconds().divide(pianolaRate)),
                    pianolaLookahead,
                    "Pianola ticker"),
            noteSpectrumBroadcast.poll()
                .toOverwritable(),
            harmonicSpectrumBroadcast.poll()
                .toOverwritable(),
            pianolaOutputBuffer,
            pianolaPattern,
            inaudibleFrequencyMargin);

        playTestTone(newNoteBuffer, spectrumWindow);
    }

    private static void playTestTone(SimpleBuffer<Frequency> newNoteBuffer, SpectrumWindow spectrumWindow) {
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
