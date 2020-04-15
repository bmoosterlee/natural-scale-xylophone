package main;

import component.Counter;
import component.Pulse;
import component.Separator;
import component.buffer.*;
import frequency.Frequency;
import gui.GUI;
import pianola.Pianola;
import pianola.notebuilder.NoteBuilder;
import pianola.patterns.PianolaPattern;
import pianola.patterns.SweepToTargetUpDown;
import sound.AmplitudeCalculator;
import sound.SampleRate;
import sound.SoundEnvironment;
import spectrum.SpectrumBuilder;
import spectrum.SpectrumWindow;
import time.Pulser;
import time.TimeInSeconds;

import java.awt.*;
import java.util.LinkedList;

class Main {

    public static void main(String[] args) {
        new TrafficAnalyzer();

        int SAMPLE_RATE = 44100/2;
        int sampleLookahead = SAMPLE_RATE / 4;
        int SAMPLE_SIZE_IN_BITS = 8;

        int frameRate = 60 / 2;
        int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();

        double octaveRange = 3.;
        int inaudibleFrequencyMargin = (int) (width / octaveRange / 12 / 5);

        int pianolaRate = 4;
        int pianolaLookahead = pianolaRate / 4;

        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        SpectrumWindow spectrumWindow = new SpectrumWindow(width, octaveRange);

        build(sampleLookahead, SAMPLE_SIZE_IN_BITS, sampleRate, frameRate, spectrumWindow, inaudibleFrequencyMargin, pianolaRate, pianolaLookahead);
    }

    private static void build(int sampleLookahead, int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate, int frameRate, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin, int pianolaRate, int pianolaLookahead) {
        SimpleBuffer<Pulse, SimplePacket<Pulse>> sampleTickerOutput = new SimpleBuffer<>(new OverflowStrategy<>("sample ticker overflow"));
        BoundedBuffer<Long, SimplePacket<Long>> sampleCountBuffer = sampleTickerOutput
                .performMethod(Counter.build(), sampleRate.sampleRate / 32, "count samples");

        LinkedList<SimpleBuffer<Long, SimplePacket<Long>>> sampleCountBroadcast = new LinkedList<>(sampleCountBuffer.broadcast(2, 100, "main - stamped samples buffer broadcast"));

        SimpleBuffer<Frequency, SimplePacket<Frequency>> newNoteBuffer = new SimpleBuffer<>(64, "new notes");

        BoundedBuffer<Double[], SimplePacket<Double[]>> volumeBuffer = NoteBuilder.buildComponent(newNoteBuffer, sampleRate, spectrumWindow, sampleCountBroadcast.poll());

        BoundedBuffer<Double[], SimplePacket<Double[]>> amplitudeStateBuffer = sampleCountBroadcast.poll().connectTo(AmplitudeCalculator.buildPipe(sampleRate, spectrumWindow));

        LinkedList<SimpleBuffer<Double[], SimplePacket<Double[]>>> volumeBroadcastAudio = new LinkedList<>(volumeBuffer.broadcast(2, "main note spectrum - broadcast"));
        LinkedList<SimpleBuffer<Double[], SimplePacket<Double[]>>> volumeBroadcastOverwritable = new LinkedList<>(volumeBroadcastAudio.poll().toOverwritable("main - volume spectrum overflow").broadcast(2, "main note spectrum overwritable - broadcast"));

        LinkedList<SimpleBuffer<Double[], SimplePacket<Double[]>>> harmonicSpectrumBroadcastAudio = new LinkedList<>(SpectrumBuilder.buildHarmonicSpectrumPipe(volumeBroadcastAudio.poll(), spectrumWindow, sampleRate).broadcast(2, "main harmonic spectrum - broadcast"));
        LinkedList<SimpleBuffer<Double[], SimplePacket<Double[]>>> harmonicSpectrumBroadcastOverwritable = new LinkedList<>(harmonicSpectrumBroadcastAudio.poll().toOverwritable("main - harmonic spectrum overflow").broadcast(2, "main harmonic spectrum overwritable - broadcast"));

        SoundEnvironment.buildComponent(harmonicSpectrumBroadcastAudio.poll(), amplitudeStateBuffer, SAMPLE_SIZE_IN_BITS, sampleRate, sampleLookahead);

        SimpleBuffer<Pulse, SimplePacket<Pulse>> guiTickerOutput = new SimpleBuffer<>(new OverwritableStrategy<>("main - dump GUI ticker overflow"));
        SimpleBuffer<java.util.List<Frequency>, ? extends Packet<java.util.List<Frequency>>> guiOutputBuffer = new SimpleBuffer<>(1, "gui output");
        guiOutputBuffer.connectTo(Separator.buildPipe()).relayTo(newNoteBuffer);
        new GUI<>(
                guiTickerOutput,
                volumeBroadcastOverwritable.poll().toOverwritable("main - dump spectrum volume input overflow"),
                harmonicSpectrumBroadcastOverwritable.poll().toOverwritable("main - dump spectrum harmonic input overflow"),
                guiOutputBuffer,
                spectrumWindow,
                inaudibleFrequencyMargin
        );

        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);

        SimpleBuffer<Pulse, SimplePacket<Pulse>> pianolaTickerOutput = new SimpleBuffer<>(new OverwritableStrategy<>("main - dump pianola ticker overflow"));
        SimpleBuffer<java.util.List<Frequency>, ? extends Packet<java.util.List<Frequency>>> pianolaOutputBuffer = new SimpleBuffer<>(1, "gui output");
        pianolaOutputBuffer.connectTo(Separator.buildPipe()).relayTo(newNoteBuffer);
        new Pianola<>(
                pianolaTickerOutput,
                volumeBroadcastOverwritable.poll()
                        .toOverwritable("main - dump pianola note spectrum input overflow"),
                harmonicSpectrumBroadcastOverwritable.poll()
                        .toOverwritable("main - dump pianola harmonic spectrum input overflow"),
                pianolaOutputBuffer,
                pianolaPattern,
                inaudibleFrequencyMargin);

        new TickRunningStrategy(new Pulser(guiTickerOutput, new TimeInSeconds(1).toNanoSeconds().divide(frameRate)));
        new TickRunningStrategy(new Pulser(pianolaTickerOutput, new TimeInSeconds(1).toNanoSeconds().divide(pianolaRate)));
        new TickRunningStrategy(new Pulser(sampleTickerOutput, new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate)));

//        playTestTone(newNoteBuffer, spectrumWindow);
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
