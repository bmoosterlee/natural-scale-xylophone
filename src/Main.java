public class Main {

    public static void main(String[] args){

        int SAMPLE_SIZE_IN_BITS = 8;
        int SAMPLE_RATE = 44100;

        NoteEnvironment noteEnvironment = new NoteEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE);
        HarmonicCalculator harmonicCalculator = new HarmonicCalculator(noteEnvironment);

        harmonicCalculator.start();
        noteEnvironment.start();

        Note testTone = new Note(440., 0);
        Note testTone2 = new Note(1100., (long) (SAMPLE_RATE * 0.5));
        noteEnvironment.addNote(testTone);
        noteEnvironment.addNote(testTone2);

    }

}
