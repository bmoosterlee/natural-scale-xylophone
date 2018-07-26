public class Main {

    public static void main(String[] args){

        int SAMPLE_SIZE_IN_BITS = 8;
        int SAMPLE_RATE = 44100;

        new PerformanceTracker();
        PerformanceTracker.start();
        NoteEnvironment noteEnvironment = new NoteEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE);
        HarmonicCalculator harmonicCalculator = new HarmonicCalculator(noteEnvironment);
        GUI gui = new GUI(noteEnvironment, harmonicCalculator);

        noteEnvironment.start();
        gui.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        noteEnvironment.addNote(gui.centerFrequency, noteEnvironment.getExpectedSampleCount());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
