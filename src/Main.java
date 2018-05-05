import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.LinkedList;

public class Main {

    public static final int SAMPLE_SIZE_IN_BITS = 8;
    public static final int SAMPLE_RATE = 44100;

    public static void main(String[] args){

        byte[] clipBuffer = new byte[ 1 ];

        AudioFormat af = new AudioFormat( (float ) SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, 1, true, false );
        SourceDataLine sdl = null;
        try {
            sdl = AudioSystem.getSourceDataLine( af );
            sdl.open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        sdl.start();

        Note testTone = new Note(440., 0);
        Note testTone2 = new Note(1100., (long)(SAMPLE_RATE * 0.5));

        LinkedList<Note> liveNotes = new LinkedList<Note>();
        liveNotes.add(testTone);
        liveNotes.add(testTone2);

        long tick = 0l;
        while(true){
            clipBuffer[ 0 ] = 0;
            for(Note note : liveNotes) {
                addAmplitude(clipBuffer, SAMPLE_RATE, note, tick);
            }
            sdl.write( clipBuffer, 0, 1 );
            tick++;
        }
//        sdl.drain();
//        sdl.stop();

    }

    private static void addAmplitude(byte[] clipBuffer, int sampleRate, Note note, long tick) {
        byte amplitude = note.getAmplitude(sampleRate, tick);
        clipBuffer[ 0 ] = (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, clipBuffer[ 0 ] + amplitude));
    }

}
