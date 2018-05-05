import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.LinkedList;

public class Main {

    public static void main(String[] args){

        byte[] clipBuffer = new byte[ 1 ];
        int sampleRate = 44100;

        AudioFormat af = new AudioFormat( (float ) sampleRate, 8, 1, true, false );
        SourceDataLine sdl = null;
        try {
            sdl = AudioSystem.getSourceDataLine( af );
            sdl.open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        sdl.start();

        Note testTone = new Note(440., 0);
        Note testTone2 = new Note(1100., (long)(sampleRate * 0.5));

        LinkedList<Note> liveNotes = new LinkedList<Note>();
        liveNotes.add(testTone);
        liveNotes.add(testTone2);

        long tick = 0l;
        while(true){
            clipBuffer[ 0 ] = 0;
            for(Note note : liveNotes) {
                addAmplitude(clipBuffer, sampleRate, note, tick);
            }
            sdl.write( clipBuffer, 0, 1 );
            tick++;
        }
//        sdl.drain();
//        sdl.stop();

    }

    private static void addAmplitude(byte[] clipBuffer, int sampleRate, Note testTone, long tick) {
        byte amplitude = testTone.getAmplitude(sampleRate, tick);
        clipBuffer[ 0 ] = (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, clipBuffer[ 0 ] + amplitude));
    }

}
