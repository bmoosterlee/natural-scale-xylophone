package main;

import gui.GUI;
import gui.spectrum.state.SpectrumManager;
import harmonics.HarmonicCalculator;
import notes.envelope.EnvelopeManager;
import notes.state.AmplitudeCalculator;
import frequency.state.FrequencyManager;
import notes.state.NoteManager;
import wave.state.WaveManager;
import pianola.Pianola;
import sound.SampleTicker;
import sound.SoundEnvironment;
import time.PerformanceTracker;

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

        BoundedBuffer<Double> sampleAmplitude = new BoundedBuffer<>(SAMPLE_RATE/2);
        SoundEnvironment soundEnvironment = new SoundEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE, sampleAmplitude);
        SampleTicker sampleTicker = new SampleTicker(soundEnvironment.getSampleRate());

        NoteManager noteManager = new NoteManager(sampleTicker);
        EnvelopeManager envelopeManager = new EnvelopeManager(noteManager, soundEnvironment.getSampleRate());
        FrequencyManager frequencyManager = new FrequencyManager(noteManager);
        WaveManager waveManager = new WaveManager(frequencyManager, soundEnvironment.getSampleRate());

        AmplitudeCalculator amplitudeCalculator = new AmplitudeCalculator(frequencyManager, envelopeManager, waveManager, sampleAmplitude);
        sampleTicker.getTickObservable().add((Observer<Long>) amplitudeCalculator::tick);

        SpectrumManager spectrumManager = new SpectrumManager();
        HarmonicCalculator harmonicCalculator = new HarmonicCalculator();
        GUI gui = new GUI(sampleTicker, harmonicCalculator, noteManager, frequencyManager, envelopeManager, spectrumManager);

        Pianola pianola = new Pianola(sampleTicker, gui, spectrumManager, noteManager, gui.spectrumWindow, 1000000000 / 4);
        //todo create a complimentary pianola pattern which, at a certain rate, checks what notes are being played,
        //todo and plays harmonically complimentary notes near the notes being played. Use a higher framerate preferably

        sampleTicker.start();
        gui.start();
        pianola.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        noteManager.addNote(gui.spectrumWindow.getCenterFrequency());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
