package main;

import component.Separator;
import component.buffer.BoundedBuffer;
import component.buffer.OutputComponentChainLink;
import component.buffer.OutputPort;
import component.buffer.SimpleBuffer;
import frequency.Frequency;
import gui.GUI;
import mixer.Mixer;
import mixer.state.AmplitudeState;
import mixer.state.VolumeAmplitudeState;
import mixer.state.VolumeState;
import pianola.Pianola;
import pianola.patterns.PianolaPattern;
import pianola.patterns.SweepToTargetUpDown;
import sound.SampleRate;
import sound.SoundEnvironment;
import spectrum.SpectrumBuilder;
import spectrum.SpectrumWindow;
import spectrum.buckets.Buckets;
import time.PerformanceTracker;
import time.Pulser;
import time.TimeInSeconds;

import java.awt.*;
import java.util.AbstractMap;
import java.util.LinkedList;

class Main {

    public static void main(String[] args){
        new PerformanceTracker();
        PerformanceTracker.start();

        int SAMPLE_RATE = 44100/256;
        int sampleLookahead = SAMPLE_RATE / 4;
        int SAMPLE_SIZE_IN_BITS = 8;

        int frameRate = 60/6;
        int frameLookahead = frameRate / 4;
        int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2;

        double octaveRange = 3.;
        int inaudibleFrequencyMargin = (int) (width/octaveRange/12/5);

        int pianolaRate = 4;
        int pianolaLookahead = pianolaRate / 4;

        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        SpectrumWindow spectrumWindow = new SpectrumWindow(width, octaveRange);

        SimpleBuffer<Frequency> newNoteBuffer = new SimpleBuffer<>(64, "new notes");

        AbstractMap.SimpleImmutableEntry<SimpleBuffer<VolumeState>, SimpleBuffer<AmplitudeState>> volumeAmplitudeStateBuffers = Mixer.buildComponent(
                OutputComponentChainLink.buildOutputBuffer(Pulser.build(new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate)),
                        sampleLookahead,
                        "sample ticker - output"),
                newNoteBuffer,
                sampleRate);

        LinkedList<SimpleBuffer<VolumeState>> volumeBroadcast =
            new LinkedList<>(volumeAmplitudeStateBuffers.getKey()
                .broadcast(2, "main volume - broadcast"));

        volumeBroadcast.poll()
        .pairWith(volumeAmplitudeStateBuffers.getValue())
        .performMethod(input -> new VolumeAmplitudeState(input.getKey(), input.getValue()))
        .connectTo(SoundEnvironment.buildPipe(SAMPLE_SIZE_IN_BITS, sampleRate));

        AbstractMap.SimpleImmutableEntry<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> spectrumPair =
            SpectrumBuilder.buildComponent(
                OutputComponentChainLink.buildOutputBuffer(Pulser.build(new TimeInSeconds(1).toNanoSeconds().divide(frameRate)), frameLookahead, "GUI ticker"),
            volumeBroadcast.poll()
                .toOverwritable(),
            spectrumWindow,
            width);

        LinkedList<SimpleBuffer<Buckets>> noteSpectrumBroadcast = new LinkedList<>(spectrumPair.getKey().broadcast(2, "main note spectrum - broadcast"));
        LinkedList<SimpleBuffer<Buckets>> harmonicSpectrumBroadcast = new LinkedList<>(spectrumPair.getValue().broadcast(2, "main harmonic spectrum - broadcast"));

        SimpleBuffer<java.util.List<Frequency>> guiOutputBuffer = new SimpleBuffer<>(1, "gui output");
        Separator.separate(guiOutputBuffer).relayTo(newNoteBuffer);
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
        Separator.separate(pianolaOutputBuffer).relayTo(newNoteBuffer);

        new Pianola(
                OutputComponentChainLink.buildOutputBuffer(Pulser.build(new TimeInSeconds(1).toNanoSeconds().divide(pianolaRate)),
                    pianolaLookahead,
                    "Pianola ticker")
                .toOverwritable(),
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
