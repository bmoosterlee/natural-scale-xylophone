package main;

import frequency.Frequency;
import gui.GUI;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumData;
import gui.spectrum.state.SpectrumManager;
import gui.spectrum.state.SpectrumState;
import harmonics.HarmonicCalculator;
import notes.state.VolumeCalculator;
import notes.state.VolumeState;
import notes.state.AmplitudeCalculator;
import pianola.Pianola;
import sound.SampleTicker;
import sound.SoundEnvironment;
import time.PerformanceTracker;
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

        BoundedBuffer<SpectrumData> spectrumInputBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<SpectrumState> spectrumStateGUIBuffer = new BoundedBuffer<>(1);
        HarmonicCalculator harmonicCalculator = new HarmonicCalculator();
        GUI gui = new GUI(spectrumInputBuffer, spectrumStateGUIBuffer, newNoteBuffer);

        SpectrumWindow spectrumWindow = gui.spectrumWindow;

        BoundedBuffer<SpectrumState> spectrumStateMultiplexerInputBuffer = new BoundedBuffer<>(1);
        OverwritableBuffer<SpectrumState> spectrumStatePianolaBuffer = new OverwritableBuffer<>(1);
        new Multiplexer<>(spectrumStateMultiplexerInputBuffer, new HashSet<>(Arrays.asList(spectrumStateGUIBuffer, spectrumStatePianolaBuffer)));
        new SpectrumManager(spectrumWindow, harmonicCalculator, spectrumInputBuffer, volumeStateBuffer3, spectrumStateMultiplexerInputBuffer);

        SampleTicker sampleTicker = new SampleTicker(soundEnvironment.getSampleRate());
        sampleTicker.getTickObservable().add(event -> {
            try {
                new OutputPort<>(sampleCountBuffer).produce(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        sampleTicker.start();

        new Pianola(spectrumStatePianolaBuffer, spectrumWindow, new TimeInSeconds(1.).toNanoSeconds().divide(4), newNoteBuffer);
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
