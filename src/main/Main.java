package main;

import component.Pulse;
import component.Separator;
import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.Orderer;
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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedList;

class Main {

    public static void main(String[] args){
        new PerformanceTracker();
        new TrafficAnalyzer();

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

        build(sampleLookahead, SAMPLE_SIZE_IN_BITS, sampleRate, frameRate, frameLookahead, spectrumWindow, inaudibleFrequencyMargin, pianolaRate, pianolaLookahead);
    }

    private static void build(int sampleLookahead, int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate, int frameRate, int frameLookahead, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin, int pianolaRate, int pianolaLookahead) {
        SimpleBuffer<Pulse, SimplePacket<Pulse>> mixerPulserRelay = new SimpleBuffer<>(1, "mixer - pulser");
        SimpleBuffer<Frequency, SimplePacket<Frequency>> newNoteBuffer = new SimpleBuffer<>(64, "new notes");

        SimpleImmutableEntry<BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>>, BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>>> volumeAmplitudeStateBuffers = Mixer.buildComponent(
                mixerPulserRelay,
                newNoteBuffer,
                sampleRate);

        LinkedList<SimpleBuffer<VolumeState, OrderStampedPacket<VolumeState>>> volumeBroadcast =
            new LinkedList<>(volumeAmplitudeStateBuffers.getKey()
                .broadcast(2, "main volume - broadcast"));

        volumeBroadcast.poll()
                .connectTo(Orderer.buildPipe("main - sample volume orderer"))
                .pairWith(
                        volumeAmplitudeStateBuffers.getValue()
                        .connectTo(Orderer.buildPipe("main - sample amplitude orderer")),
                        "main - pair volume and amplitude")
                .<VolumeAmplitudeState, OrderStampedPacket<VolumeAmplitudeState>>performMethod(input ->
                                new VolumeAmplitudeState(
                                        input.getKey(),
                                        input.getValue())
                        , "main - construct volume amplitude state")
                .connectTo(SoundEnvironment.buildPipe(SAMPLE_SIZE_IN_BITS, sampleRate, sampleLookahead));

        AbstractMap.SimpleImmutableEntry<BoundedBuffer<Buckets, SimplePacket<Buckets>>, BoundedBuffer<Buckets, SimplePacket<Buckets>>> spectrumPair =
            SpectrumBuilder.buildComponent(
                OutputComponentChainLink.buildOutputBuffer(Pulser.build(new TimeInSeconds(1).toNanoSeconds().divide(frameRate)), frameLookahead, "GUI ticker"),
            volumeBroadcast.poll()
                .rewrap()
                .toOverwritable(),
            spectrumWindow);

        LinkedList<SimpleBuffer<Buckets, ? extends Packet<Buckets>>> noteSpectrumBroadcast = new LinkedList<>(spectrumPair.getKey().broadcast(2, "main note spectrum - broadcast"));
        LinkedList<SimpleBuffer<Buckets, ? extends Packet<Buckets>>> harmonicSpectrumBroadcast = new LinkedList<>(spectrumPair.getValue().broadcast(2, "main harmonic spectrum - broadcast"));

        SimpleBuffer<java.util.List<Frequency>, ? extends Packet<java.util.List<Frequency>>> guiOutputBuffer = new SimpleBuffer<>(1, "gui output");
        guiOutputBuffer.connectTo(Separator.buildPipe()).relayTo(newNoteBuffer);
        new GUI(
            noteSpectrumBroadcast.poll(),
            harmonicSpectrumBroadcast.poll(),
            guiOutputBuffer,
            spectrumWindow,
            inaudibleFrequencyMargin
        );

//        PianolaPattern pianolaPattern = new Sweep(this, 8, spectrumWindow.getCenterFrequency());
//        PianolaPattern pianolaPattern = new PatternPauser(8, new SweepToTarget(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, 5, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow), 5);
        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);
//        PianolaPattern pianolaPattern = new SimpleArpeggio(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer,3, spectrumWindow);


        SimpleBuffer<java.util.List<Frequency>, ? extends Packet<java.util.List<Frequency>>> pianolaOutputBuffer = new SimpleBuffer<>(1, "gui output");
        pianolaOutputBuffer.connectTo(Separator.buildPipe()).relayTo(newNoteBuffer);

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

        OutputComponentChainLink.buildOutputBuffer(Pulser.build(new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate)),
                sampleLookahead,
                "sample ticker - output")
                .relayTo(mixerPulserRelay);

        playTestTone(newNoteBuffer, spectrumWindow);
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
