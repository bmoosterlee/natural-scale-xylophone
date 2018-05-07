import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.LinkedList;

public class Main {

    public static final int SAMPLE_SIZE_IN_BITS = 8;
    public static final int SAMPLE_RATE = 44100;

    private static byte[] clipBuffer;
    private static SourceDataLine sdl;
    private static LinkedList<Note> liveNotes;
    private static LinkedList<Note> notesToBeRemoved;
    private static long tick;

    public static void main(String[] args){

        initialize();

        Note testTone = new Note(440., 0);
        Note testTone2 = new Note(1100., (long)(SAMPLE_RATE * 0.5));
        liveNotes.add(testTone);
        liveNotes.add(testTone2);

        run();
        close();

    }

    private static void close() {
        sdl.drain();
        sdl.stop();
    }

    private static void run() {
        tick = 0l;
        while(true){
            getClipBuffer()[ 0 ] = 0;

            for(Note note : liveNotes) {
                if(note.isDead(tick)){
                    notesToBeRemoved.add(note);
                }
                addAmplitude(getClipBuffer(), SAMPLE_RATE, note, tick);
            }
            liveNotes.remove(notesToBeRemoved);
            notesToBeRemoved.clear();

            getSdl().write(getClipBuffer(), 0, 1 );
            tick++;
        }
    }

    private static void initialize() {
        setClipBuffer(new byte[ 1 ]);

        AudioFormat af = new AudioFormat( (float ) SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, 1, true, false );
        setSdl(null);
        try {
            setSdl(AudioSystem.getSourceDataLine( af ));
            getSdl().open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        getSdl().start();

        liveNotes = new LinkedList<>();
        notesToBeRemoved = new LinkedList<>();
    }

    private static void addAmplitude(byte[] clipBuffer, int sampleRate, Note note, long tick) {
        byte amplitude = (byte)((Math.pow(2, SAMPLE_SIZE_IN_BITS)-1)*(0.5*(1.+note.getAmplitude(sampleRate, tick)))-Math.pow(2, SAMPLE_SIZE_IN_BITS)/2);
        clipBuffer[ 0 ] = (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, clipBuffer[ 0 ] + amplitude));
    }

    public static byte[] getClipBuffer() {
        return clipBuffer;
    }

    public static void setClipBuffer(byte[] clipBuffer) {
        Main.clipBuffer = clipBuffer;
    }

    public static SourceDataLine getSdl() {
        return sdl;
    }

    public static void setSdl(SourceDataLine sdl) {
        Main.sdl = sdl;
    }
}
