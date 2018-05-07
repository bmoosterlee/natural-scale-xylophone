import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.LinkedList;

public class NoteEnvironment implements Runnable{

    private final int SAMPLE_SIZE_IN_BITS;
    private final int SAMPLE_RATE;
    private byte[] clipBuffer;
    private SourceDataLine sdl;
    private LinkedList<Note> liveNotes;
    private LinkedList<Note> notesToBeRemoved;
    private long tick;

    public NoteEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE){
        this.SAMPLE_SIZE_IN_BITS = SAMPLE_SIZE_IN_BITS;
        this.SAMPLE_RATE = SAMPLE_RATE;

        initialize();
    }

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void close() {
        sdl.drain();
        sdl.stop();
    }

    @Override
    public void run() {
        //TODO Move ticker to it's own class, which sends a message to the NoteEnvironment and HarmonicCalculator
        // at each new tick, and waits for the next tick instead of calculating as many ticks as possible, because this
        //might cause cause timing issues with notes that are played in the interface, and the ticker has moved faster
        // than the sdl, causing there to be a backlog of ticks which need to be played before our note is.
        tick = 0l;
        while (true) {
            tick();
            tick++;
        }
//        close();
    }

    private void tick() {
        getClipBuffer()[0] = 0;

        for (Note note : getLiveNotes()) {
            if (note.isDead(tick, 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS))) {
                notesToBeRemoved.add(note);
            }
            addAmplitude(getClipBuffer(), SAMPLE_RATE, note, tick);
        }
        getLiveNotes().remove(notesToBeRemoved);
        notesToBeRemoved.clear();

        getSdl().write(getClipBuffer(), 0, 1);
    }

    private void initialize() {
        setClipBuffer(new byte[1]);

        AudioFormat af = new AudioFormat((float) SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, 1, true, false);
        setSdl(null);
        try {
            setSdl(AudioSystem.getSourceDataLine(af));
            getSdl().open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        getSdl().start();

        setLiveNotes(new LinkedList<Note>());
        notesToBeRemoved = new LinkedList<Note>();
    }

    private void addAmplitude(byte[] clipBuffer, int sampleRate, Note note, long tick) {
        byte amplitude = (byte) ((Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1) * (0.5 * (1. + note.getAmplitude(sampleRate, tick))) - Math.pow(2, SAMPLE_SIZE_IN_BITS) / 2);
        clipBuffer[0] = (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, clipBuffer[0] + amplitude));
    }

    private byte[] getClipBuffer() {
        return clipBuffer;
    }

    private void setClipBuffer(byte[] clipBuffer) {
        this.clipBuffer = clipBuffer;
    }

    private SourceDataLine getSdl() {
        return sdl;
    }

    private void setSdl(SourceDataLine sdl) {
        this.sdl = sdl;
    }

    public LinkedList<Note> getLiveNotes() {
        return liveNotes;
    }

    private void setLiveNotes(LinkedList<Note> liveNotes) {
        this.liveNotes = liveNotes;
    }

    public void addNote(Note note) {
        getLiveNotes().add(note);
    }
}