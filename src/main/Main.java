package main;

import component.Pulse;
import component.Separator;
import component.buffer.*;
import component.orderer.OrderStampedPacket;
import frequency.Frequency;
import gui.GUI;
import pianola.Pianola;
import pianola.notebuilder.NoteBuilder;
import pianola.patterns.PianolaPattern;
import pianola.patterns.SweepToTargetUpDown;
import sound.*;
import spectrum.SpectrumBuilder;
import spectrum.SpectrumWindow;
import time.Pulser;
import time.TimeInSeconds;

import java.awt.*;
import java.util.AbstractMap;
import java.util.LinkedList;

class Main {

    public static void main(String[] args){
        new TrafficAnalyzer();

        int SAMPLE_RATE = 44100/2;
        int sampleLookahead = SAMPLE_RATE / 4;
        int SAMPLE_SIZE_IN_BITS = 8;

        int frameRate = 60/2;
        int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();

        double octaveRange = 3.;
        int inaudibleFrequencyMargin = (int) (width/octaveRange/12/5);

        int pianolaRate = 4;
        int pianolaLookahead = pianolaRate / 4;

        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        SpectrumWindow spectrumWindow = new SpectrumWindow(width, octaveRange);

        build(sampleLookahead, SAMPLE_SIZE_IN_BITS, sampleRate, frameRate, spectrumWindow, inaudibleFrequencyMargin, pianolaRate, pianolaLookahead);
    }

    private static void build(int sampleLookahead, int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate, int frameRate, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin, int pianolaRate, int pianolaLookahead) {
        BoundedBuffer<Long, OrderStampedPacket<Long>> stampedSamplesBuffer = SampleTicker.buildComponent(sampleRate);

        LinkedList<SimpleBuffer<Long, OrderStampedPacket<Long>>> stampedSampleBroadcast = new LinkedList<>(stampedSamplesBuffer.broadcast(2, 100, "main - stamped samples buffer broadcast"));

        SimpleBuffer<Frequency, SimplePacket<Frequency>> newNoteBuffer = new SimpleBuffer<>(64, "new notes");

        BoundedBuffer<VolumeState, OrderStampedPacket<VolumeState>> volumeBuffer = NoteBuilder.buildComponent(newNoteBuffer, sampleRate, spectrumWindow, stampedSampleBroadcast.poll());

        LinkedList<SimpleBuffer<VolumeState, OrderStampedPacket<VolumeState>>> volumeBroadcast =
            new LinkedList<>(volumeBuffer
                .broadcast(2, 100, "main volume - broadcast"));

        BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>> amplitudeStateBuffer = stampedSampleBroadcast.poll().connectTo(AmplitudeCalculator.buildPipe(sampleRate, spectrumWindow));

        SoundEnvironment.buildComponent(volumeBroadcast.poll(), amplitudeStateBuffer, SAMPLE_SIZE_IN_BITS, sampleRate, sampleLookahead);

        SimpleBuffer<Pulse, SimplePacket<Pulse>> guiTickerOutput = new SimpleBuffer<>(new OverwritableStrategy<>("main - dump GUI ticker overflow"));
        AbstractMap.SimpleImmutableEntry<BoundedBuffer<Double[], SimplePacket<Double[]>>, BoundedBuffer<Double[], SimplePacket<Double[]>>> spectrumPair =
            SpectrumBuilder.buildComponent(
            guiTickerOutput,
            volumeBroadcast.poll()
                .toOverwritable("main - dump spectrum volume input overflow"),
            spectrumWindow);

        LinkedList<SimpleBuffer<Double[], ? extends Packet<Double[]>>> noteSpectrumBroadcast = new LinkedList<>(spectrumPair.getKey().broadcast(2, "main note spectrum - broadcast"));
        LinkedList<SimpleBuffer<Double[], ? extends Packet<Double[]>>> harmonicSpectrumBroadcast = new LinkedList<>(spectrumPair.getValue().broadcast(2, "main harmonic spectrum - broadcast"));

        SimpleBuffer<java.util.List<Frequency>, ? extends Packet<java.util.List<Frequency>>> guiOutputBuffer = new SimpleBuffer<>(1, "gui output");
        guiOutputBuffer.connectTo(Separator.buildPipe()).relayTo(newNoteBuffer);
        new GUI(
            noteSpectrumBroadcast.poll(),
            harmonicSpectrumBroadcast.poll(),
            guiOutputBuffer,
            spectrumWindow,
            inaudibleFrequencyMargin
        );

        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);

        SimpleBuffer<Pulse, SimplePacket<Pulse>> pianolaTickerOutput = new SimpleBuffer<>(new OverwritableStrategy<>("main - dump pianola ticker overflow"));
        SimpleBuffer<java.util.List<Frequency>, ? extends Packet<java.util.List<Frequency>>> pianolaOutputBuffer = new SimpleBuffer<>(1, "gui output");
        pianolaOutputBuffer.connectTo(Separator.buildPipe()).relayTo(newNoteBuffer);
        new Pianola(
            pianolaTickerOutput,
            noteSpectrumBroadcast.poll()
                .toOverwritable("main - dump pianola note spectrum input overflow"),
            harmonicSpectrumBroadcast.poll()
                .toOverwritable("main - dump pianola harmonic spectrum input overflow"),
            pianolaOutputBuffer,
            pianolaPattern,
            inaudibleFrequencyMargin);

        new TickRunningStrategy(new Pulser(guiTickerOutput, new TimeInSeconds(1).toNanoSeconds().divide(frameRate)));
        new TickRunningStrategy(new Pulser(pianolaTickerOutput, new TimeInSeconds(1).toNanoSeconds().divide(pianolaRate)));

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
