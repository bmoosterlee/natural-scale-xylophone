package main;

import gui.GUI;
import harmonics.HarmonicCalculator;
import notes.state.AmplitudeCalculator;
import notes.state.NoteEnvironment;
import notes.state.NoteManager;
import notes.state.SoundEnvironment;
import pianola.Pianola;

public class Main {

    public static void main(String[] args){

        int SAMPLE_SIZE_IN_BITS = 8;
        int SAMPLE_RATE = 44100;

        new PerformanceTracker();
        PerformanceTracker.start();
        SoundEnvironment soundEnvironment = new SoundEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE);
        NoteEnvironment noteEnvironment = new NoteEnvironment(soundEnvironment);
        NoteManager noteManager = new NoteManager(noteEnvironment, soundEnvironment.getSampleRate());
        AmplitudeCalculator amplitudeCalculator = new AmplitudeCalculator(soundEnvironment, noteManager);
        noteEnvironment.tickObservers.add(amplitudeCalculator);
        HarmonicCalculator harmonicCalculator = new HarmonicCalculator();
        GUI gui = new GUI(noteEnvironment, harmonicCalculator, noteManager);

        noteEnvironment.start();
        gui.start();

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
