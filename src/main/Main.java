package main;

import gui.GUI;
import harmonics.HarmonicCalculator;
import notes.state.NoteEnvironment;
import pianola.Pianola;

public class Main {

    public static void main(String[] args){

        int SAMPLE_SIZE_IN_BITS = 8;
        int SAMPLE_RATE = 44100;

        new PerformanceTracker();
        PerformanceTracker.start();
        NoteEnvironment noteEnvironment = new NoteEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE);
        HarmonicCalculator harmonicCalculator = new HarmonicCalculator();
        GUI gui = new GUI(noteEnvironment, harmonicCalculator);

        noteEnvironment.start();
        gui.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        noteEnvironment.noteManager.addNote(gui.spectrumWindow.getCenterFrequency());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
