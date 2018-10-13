package main;

import frequency.Frequency;
import gui.GUI;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumInput;
import gui.spectrum.state.SpectrumManager;
import gui.spectrum.state.SpectrumState;
import harmonics.HarmonicCalculator;
import notes.envelope.EnvelopeManager;
import notes.state.AmplitudeCalculator;
import frequency.state.FrequencyManager;
import notes.state.NoteManager;
import sound.SampleRate;
import time.TimeInSeconds;
import wave.state.WaveManager;
import pianola.Pianola;
import sound.SampleTicker;
import sound.SoundEnvironment;
import time.PerformanceTracker;
import wave.state.WaveState;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.HashSet;

public class Main {

    public static void main(String[] args){

        /*todo move towards threads started for each frame which can quit at any moment, so that when time in the frame
        runs out, we can move on to the next frame without losing all our progress from this frame. One way to do it
        could be to stream pipeline each unit, and check after each unit whether we have time left.
            Another could be that we stream pipeline each unit, store the results persistently, and kill the thread when
        time runs out.*/
        int SAMPLE_SIZE_IN_BITS = 8;
        int SAMPLE_RATE = 44100;

        new PerformanceTracker();
        PerformanceTracker.start();

        int lookahead = SAMPLE_RATE / 20;
        BoundedBuffer<Double> sampleAmplitude = new BoundedBuffer<>(lookahead);
        SoundEnvironment soundEnvironment = new SoundEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE, sampleAmplitude);
        SampleTicker sampleTicker = new SampleTicker(soundEnvironment.getSampleRate());

        BoundedBuffer<SimpleImmutableEntry<Long, Frequency>> newNotes = new BoundedBuffer<>(10);
        NoteManager noteManager = new NoteManager(soundEnvironment.getSampleRate(), newNotes);
        EnvelopeManager envelopeManager = new EnvelopeManager(noteManager, soundEnvironment.getSampleRate());
        FrequencyManager frequencyManager = new FrequencyManager(noteManager);

        BoundedBuffer<Long> sampleCountBuffer = new BoundedBuffer<>(lookahead);
        BoundedBuffer<WaveState> waveStateBuffer = new BoundedBuffer<>(lookahead);
        new WaveManager(frequencyManager, soundEnvironment.getSampleRate(), sampleCountBuffer, waveStateBuffer);

        AmplitudeCalculator amplitudeCalculator = new AmplitudeCalculator(frequencyManager, envelopeManager, sampleAmplitude, sampleCountBuffer, waveStateBuffer);
        sampleTicker.getTickObservable().add(amplitudeCalculator::tick);
        sampleTicker.start();

        BoundedBuffer<SpectrumInput> spectrumInputBuffer = new BoundedBuffer<>(1);
        BoundedBuffer<SpectrumState> spectrumStateGUIBuffer = new BoundedBuffer<>(1);
        HarmonicCalculator harmonicCalculator = new HarmonicCalculator();
        GUI gui = new GUI(sampleTicker, spectrumInputBuffer, spectrumStateGUIBuffer, newNotes);

        SpectrumWindow spectrumWindow = gui.spectrumWindow;

        BoundedBuffer<SpectrumState> spectrumStateMultiplexerInputBuffer = new BoundedBuffer<>(1);
        OverwritableBuffer<SpectrumState> spectrumStatePianolaBuffer = new OverwritableBuffer<>(1);
        new Multiplexer<>(spectrumStateMultiplexerInputBuffer, new HashSet<>(Arrays.asList(spectrumStateGUIBuffer, spectrumStatePianolaBuffer)));

        new SpectrumManager(spectrumWindow, frequencyManager, envelopeManager, harmonicCalculator, spectrumInputBuffer, spectrumStateMultiplexerInputBuffer);

        new Pianola(sampleTicker, spectrumStatePianolaBuffer, spectrumWindow, new TimeInSeconds(1.).toNanoSeconds().divide(4), newNotes);
        //todo create a complimentary pianola pattern which, at a certain rate, checks what notes are being played,
        //todo and plays harmonically complimentary notes near the notes being played. Use a higher framerate preferably

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            new OutputPort<>(newNotes).produce(new SimpleImmutableEntry<>(sampleTicker.getExpectedTickCount(), spectrumWindow.getCenterFrequency()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
