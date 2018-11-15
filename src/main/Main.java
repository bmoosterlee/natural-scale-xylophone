package main;

import component.*;
import component.buffer.*;
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
import java.util.AbstractMap;
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
                .performMethod(Mixer.build(newNoteBuffer, sampleRate), "mix")
                .broadcast(2));

        volumeBroadcast.poll()
        .performInputMethod(SoundEnvironment.build(SAMPLE_SIZE_IN_BITS, sampleRate));

        LinkedList<SimpleBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>>> spectrumBroadcast =
            new LinkedList<>(
                RunningOutputComponent.buildOutputBuffer(
                    Ticker.build(new TimeInSeconds(1).toNanoSeconds().divide(frameRate)), frameLookahead, "GUI ticker")
                .performMethod(
                    SpectrumBuilder.build(
                        volumeBroadcast.poll()
                        .relayTo(new SimpleBuffer<>(new OverwritableStrategy<>(1, "sound - volume amplitude state out"))),
                        spectrumWindow,
                        width), "build spectrum")
                .broadcast(2));

        spectrumBroadcast.poll()
        .performMethod(GUI.build(spectrumWindow, width, inaudibleFrequencyMargin))
        .relayTo(newNoteBuffer);

//        PianolaPattern pianolaPattern = new Sweep(this, 8, spectrumWindow.getCenterFrequency());
//        PianolaPattern pianolaPattern = new PatternPauser(8, new SweepToTarget(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, 5, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow), 5);
        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);
//        PianolaPattern pianolaPattern = new SimpleArpeggio(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer,3, spectrumWindow);

        Unzipper.unzip(
            RunningOutputComponent.buildOutputBuffer(
                Ticker.build(new TimeInSeconds(1).toNanoSeconds().divide(pianolaRate)),
                pianolaLookahead,
                "Pianola ticker")
            .performMethod(
                Pianola.build(
                    spectrumBroadcast.poll()
                    .relayTo(
                        new SimpleBuffer<>(
                            new OverwritableStrategy<>(1, "pianola - input"))),
                    pianolaPattern,
                    inaudibleFrequencyMargin), "build pianola"))
        .relayTo(newNoteBuffer);

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
