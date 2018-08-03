import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class NoteEnvironment implements Runnable{

    private final int SAMPLE_SIZE_IN_BITS;
    private final int SAMPLE_RATE;
    private byte[] clipBuffer;
    private SourceDataLine sourceDataLine;
    private HashSet<Note> liveNotes;
    private long sampleCount;
    private long timeZero;

    public NoteEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE){
        this.SAMPLE_SIZE_IN_BITS = SAMPLE_SIZE_IN_BITS;
        this.SAMPLE_RATE = SAMPLE_RATE;


        liveNotes = new HashSet();

        initialize();
    }

    static HashMap<Note, Double> getVolumeTable(long currentSampleCount, Set<Note> liveNotes) {
        HashMap<Note, Double> newVolumeTable = new HashMap<>();
        for(Note note : liveNotes) {
            newVolumeTable.put(note, note.getVolume(currentSampleCount));
        }
        return newVolumeTable;
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
        sampleCount = 0l;
        timeZero = System.nanoTime();
        double frameTime = 1000000000./SAMPLE_RATE;
        int sampleLookahead = SAMPLE_RATE/100;
        long sampleBacklog = 0;

        while(true) {
            while(sampleBacklog>0) {
                TimeKeeper timeKeeper = PerformanceTracker.startTracking("NoteEnvironment tick");
                tick();
                PerformanceTracker.stopTracking(timeKeeper);
                sampleBacklog--;
            }

            long expectedSampleCount = getExpectedSampleCount();
            sampleBacklog = expectedSampleCount+sampleLookahead- sampleCount;
            long waitTime = (long) ((-sampleBacklog)*frameTime);

            long waitTimeMillis = (long)(frameTime*sampleLookahead)/1000000;
            int waitTimeNanos = (int) (frameTime*sampleLookahead)%1000;
            if(waitTime>0){
                try {
                    Thread.sleep(waitTimeMillis,waitTimeNanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
//        close();
    }

    private void tick() {
        removeInaudibleNotes();
        clipBuffer[0] = calculateAmplitudeSum();
        getSourceDataLine().write(getClipBuffer(), 0, 1);
        sampleCount++;
    }

    private byte calculateAmplitudeSum() {
        byte amplitudeSum = 0;
        HashSet<Note> currentLiveNotes = getLiveNotes();

        for (Note note : currentLiveNotes) {
            byte amplitude = getAmplitude(note);
            amplitudeSum = (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, amplitudeSum + amplitude));
        }
        return amplitudeSum;
    }

    private void removeInaudibleNotes() {
        HashSet<Note> currentLiveNotes = getLiveNotes();
        for (Note note : currentLiveNotes) {
            if (note.getVolume(sampleCount) < 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS)) {
                synchronized(liveNotes) {
                    liveNotes.remove(note);
                }
            }
        }
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
    }

    private byte getAmplitude(Note note) {
        int sampleSize = (int)(Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        byte amplitude = (byte) Math.floor(sampleSize * note.getAmplitude(sampleCount) / 2);
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

    public HashSet<Note> getLiveNotes() {
        synchronized(liveNotes) {
            return new HashSet<>(liveNotes);
        }
    }

    public void addNote(double frequency, long startingSampleCount) {
        Note note = new Note(frequency, startingSampleCount, SAMPLE_RATE);
        synchronized(liveNotes) {
            liveNotes.add(note);
        }
    }

    public long getExpectedSampleCount() {
        return (long)((System.nanoTime()- timeZero) / 1000000000. * SAMPLE_RATE);
    }

    public void addNote(double frequency) {
        addNote(frequency, getExpectedSampleCount());
    }
}
