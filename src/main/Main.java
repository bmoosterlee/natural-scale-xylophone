package main;

import gui.GUI;
import harmonics.HarmonicCalculator;
import notes.state.AmplitudeCalculator;
import sound.SampleTicker;
import notes.state.NoteManager;
import sound.SoundEnvironment;
import time.PerformanceTracker;
import pianola.Pianola;

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
        SoundEnvironment soundEnvironment = new SoundEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE);
        SampleTicker sampleTicker = new SampleTicker(soundEnvironment.getSampleRate());
        NoteManager noteManager = new NoteManager(sampleTicker, soundEnvironment.getSampleRate());
        AmplitudeCalculator amplitudeCalculator = new AmplitudeCalculator(soundEnvironment, noteManager);
        sampleTicker.getTickObservable().add((Observer<Long>) event -> amplitudeCalculator.tick(event));

        HarmonicCalculator harmonicCalculator = new HarmonicCalculator();
        GUI gui = new GUI(sampleTicker, harmonicCalculator, noteManager);

        Pianola pianola = new Pianola(sampleTicker, gui, noteManager, 1000000000 / 4);

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
