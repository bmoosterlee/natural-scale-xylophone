public class Main {

    public static void main(String[] args){

        int SAMPLE_SIZE_IN_BITS = 8;
        int SAMPLE_RATE = 44100;

        NoteEnvironment noteEnvironment = new NoteEnvironment(SAMPLE_SIZE_IN_BITS, SAMPLE_RATE);

        Note testTone = new Note(440., 0);
        Note testTone2 = new Note(1100., (long) (SAMPLE_RATE * 0.5));
        noteEnvironment.getLiveNotes().add(testTone);
        noteEnvironment.getLiveNotes().add(testTone2);

        noteEnvironment.run();
        noteEnvironment.close();

    }

}
