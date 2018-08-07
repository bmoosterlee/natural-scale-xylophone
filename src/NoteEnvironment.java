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
    private SourceDataLine sourceDataLine;
    private HashSet<Note> liveNotes;
    private long calculatedSamples;
    private long timeZero;
    private int sampleSize;
    private double marginalSampleSize;
    private double frameTime;
    private int sampleLookahead;

    public NoteEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE){
        this.SAMPLE_SIZE_IN_BITS = SAMPLE_SIZE_IN_BITS;
        this.SAMPLE_RATE = SAMPLE_RATE;

        sampleSize = (int)(Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

        frameTime = 1000000./SAMPLE_RATE;
        sampleLookahead = SAMPLE_RATE/10;

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
        calculatedSamples = 0l;
        timeZero = System.nanoTime();
        long sampleBacklog = 0;

        while(true) {
            long startTime = System.nanoTime();

            while(sampleBacklog>0) {
                tick();
                sampleBacklog--;
            }

            long expectedSampleCount = getExpectedSampleCount();
            sampleBacklog = expectedSampleCount + sampleLookahead - calculatedSamples;

            TimeKeeper sleepTimeKeeper = PerformanceTracker.startTracking("NoteEnvironment sleep");

            long timeLeftInFrame = getTimeLeftInFrame(startTime);

            if(timeLeftInFrame>0){
                try {
                    Thread.sleep(timeLeftInFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            PerformanceTracker.stopTracking(sleepTimeKeeper);
        }
//        close();
    }

    private void tick() {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("NoteEnvironment removeInaudibleNotes");
        removeInaudibleNotes();
        PerformanceTracker.stopTracking(timeKeeper);

        TimeKeeper amplitudeTimeKeeper = PerformanceTracker.startTracking("NoteEnvironment calculateAmplitudes");
        byte[] clipBuffer = new byte[]{calculateAmplitudeSum()};
        PerformanceTracker.stopTracking(amplitudeTimeKeeper);

        TimeKeeper bufferTimeKeeper = PerformanceTracker.startTracking("NoteEnvironment writeToBuffer");
        getSourceDataLine().write(clipBuffer, 0, 1);
        calculatedSamples++;
        PerformanceTracker.stopTracking(bufferTimeKeeper);
    }

    private byte calculateAmplitudeSum() {
        HashSet<Note> currentLiveNotes = getLiveNotes();

        int amplitudeSum = 0;
        for (Note note : currentLiveNotes) {
            amplitudeSum += getAmplitude(note);
        }
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, amplitudeSum));
    }

    private void removeInaudibleNotes() {
        HashSet<Note> currentLiveNotes = getLiveNotes();
        HashSet<Note> notesToBeRemoved = new HashSet<>();
        for (Note note : currentLiveNotes) {
            if (note.getVolume(calculatedSamples) < marginalSampleSize) {
                notesToBeRemoved.add(note);
            }
        }
        synchronized (liveNotes){
            liveNotes.removeAll(notesToBeRemoved);
        }
    }

    private void initialize() {
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
        return (byte) Math.floor(sampleSize * note.getAmplitude(calculatedSamples) / 2);
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

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);

        return (long) ((frameTime*sampleLookahead - timePassed)/ 1000000);
    }
}
