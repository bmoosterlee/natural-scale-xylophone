import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.LinkedList;

public class NoteEnvironment implements Runnable{

    private final int SAMPLE_SIZE_IN_BITS;
    private final int SAMPLE_RATE;
    private byte[] clipBuffer;
    private SourceDataLine sourceDataLine;
    private LinkedList<Note> liveNotes;
    private LinkedList<Note> notesToBeRemoved;
    private long sampleCount;

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
        sourceDataLine.drain();
        sourceDataLine.stop();
    }

    @Override
    public void run() {
        //TODO Move ticker to it's own class, which sends a message to the NoteEnvironment and HarmonicCalculator
        // at each new sampleCount, and waits for the next sampleCount instead of calculating as many ticks as possible, because this
        //might cause cause timing issues with notes that are played in the interface, and the ticker has moved faster
        // than the sourceDataLine, causing there to be a backlog of ticks which need to be played before our note is.

        sampleCount = 0l;
        long timeZero = System.nanoTime();
        long frameTime = 1000000000/SAMPLE_RATE;
        int tickLookahead = (int) (frameTime*1000);

        while(true) {
            removeInaudibleNotes();
            clipBuffer[0] = calculateAmplitudeSum();
            writeSample();

            long expectedTickCount = (System.nanoTime()-timeZero) * SAMPLE_RATE;
            if(sampleCount > expectedTickCount+tickLookahead){
                try {
                    Thread.sleep(0,tickLookahead/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
//        close();
    }

    private void writeSample() {
        getSourceDataLine().write(getClipBuffer(), 0, 1);
        sampleCount++;
    }

    private byte calculateAmplitudeSum() {
        byte amplitudeSum = 0;
        LinkedList<Note> currentLiveNotes = (LinkedList<Note>) getLiveNotes().clone();

        for (Note note : currentLiveNotes) {
            byte amplitude = getAmplitude(getClipBuffer(), SAMPLE_RATE, note, sampleCount);
            amplitudeSum = (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, amplitudeSum + amplitude));
        }
        return amplitudeSum;
    }

    private void removeInaudibleNotes() {
        LinkedList<Note> currentLiveNotes = (LinkedList<Note>) getLiveNotes().clone();
        for (Note note : currentLiveNotes) {
            if (note.isInaudible(sampleCount, 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS))) {
                notesToBeRemoved.add(note);
            }
        }
        getLiveNotes().remove(notesToBeRemoved);
        notesToBeRemoved.clear();
    }

    private void initialize() {
        setClipBuffer(new byte[1]);

        AudioFormat af = new AudioFormat((float) SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, 1, true, false);
        setSourceDataLine(null);
        try {
            setSourceDataLine(AudioSystem.getSourceDataLine(af));
            getSourceDataLine().open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        getSourceDataLine().start();

        setLiveNotes(new LinkedList<Note>());
        notesToBeRemoved = new LinkedList<Note>();
    }

    private byte getAmplitude(byte[] clipBuffer, int sampleRate, Note note, long sampleCount) {
        int sampleSize = (int)(Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        byte amplitude = (byte) Math.floor(sampleSize * note.getAmplitude(sampleRate, sampleCount) / 2);
        return amplitude;
    }

    private byte[] getClipBuffer() {
        return clipBuffer;
    }

    private void setClipBuffer(byte[] clipBuffer) {
        this.clipBuffer = clipBuffer;
    }

    private SourceDataLine getSourceDataLine() {
        return sourceDataLine;
    }

    private void setSourceDataLine(SourceDataLine sourceDataLine) {
        this.sourceDataLine = sourceDataLine;
    }

    public synchronized LinkedList<Note> getLiveNotes() {
        return liveNotes;
    }

    private synchronized void setLiveNotes(LinkedList<Note> liveNotes) {
        this.liveNotes = liveNotes;
    }

    public void addNote(Note note) {
        getLiveNotes().add(note);
    }

    public long sampleCount() {
        return sampleCount;
    }
}