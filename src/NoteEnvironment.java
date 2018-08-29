import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NoteEnvironment implements Runnable{

    private final int SAMPLE_SIZE_IN_BITS;
    private final SampleRate sampleRate;
    final NoteManager noteManager;
    private SourceDataLine sourceDataLine;
    private long calculatedSamples;
    private long timeZero;
    private int sampleSize;
    private double marginalSampleSize;
    private double frameTime;
    private int sampleLookahead;

    NoteEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE){
        this.SAMPLE_SIZE_IN_BITS = SAMPLE_SIZE_IN_BITS;
        sampleRate = new SampleRate(SAMPLE_RATE);
        noteManager = new NoteManager(this);

        sampleSize = (int)(Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

        frameTime = 1000000./SAMPLE_RATE;
        sampleLookahead = SAMPLE_RATE/10;

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
        calculatedSamples = 0L;
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
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("NoteEnvironment getLiveNotes");
        HashSet<Note> liveNotes = noteManager.getLiveNotes();
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("NoteEnvironment getVolumeTable");
        HashMap<Note, Double> volumeTable = getVolumeTable(liveNotes);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("NoteEnvironment getInaudibleNotes");
        HashSet<Note> inaudibleNotes = noteManager.getInaudibleNotes(volumeTable, liveNotes);
        noteManager.removeInaudibleNotes(inaudibleNotes);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("NoteEnvironment calculateAmplitudes");
        byte[] clipBuffer = new byte[]{calculateAmplitudeSum(volumeTable, liveNotes)};
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("NoteEnvironment writeToBuffer");
        getSourceDataLine().write(clipBuffer, 0, 1);
        calculatedSamples++;
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private byte calculateAmplitudeSum(HashMap<Note, Double> volumeTable, HashSet<Note> currentLiveNotes) {
        int amplitudeSum = 0;
        for (Note note : currentLiveNotes) {
            amplitudeSum += getAmplitude(note, volumeTable.get(note));
        }
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, amplitudeSum));
    }

    boolean isAudible(Double volume) {
        return volume >= marginalSampleSize;
    }

    private HashMap<Note, Double> getVolumeTable(HashSet<Note> currentLiveNotes) {
        HashMap<Note, Double> volumeTable = new HashMap<>();
        for (Note note : currentLiveNotes) {
            volumeTable.put(note, note.getVolume(calculatedSamples));
        }
        return volumeTable;
    }

    private void initialize() {
        AudioFormat af = new AudioFormat((float) sampleRate.sampleRate, SAMPLE_SIZE_IN_BITS, 1, true, false);
        setSourceDataLine(null);
        try {
            setSourceDataLine(AudioSystem.getSourceDataLine(af));
            getSourceDataLine().open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        getSourceDataLine().start();
    }

    private byte getAmplitude(Note note, double volume) {
        return (byte) Math.floor(sampleSize * note.getAmplitude(calculatedSamples, volume) / 2);
    }

    private SourceDataLine getSourceDataLine() {
        return sourceDataLine;
    }

    private void setSourceDataLine(SourceDataLine sourceDataLine) {
        this.sourceDataLine = sourceDataLine;
    }

    public HashSet<Note> getLiveNotes() {
        return noteManager.getLiveNotes();
    }

    Note createNote(double frequency) {
        return new Note(frequency, getExpectedSampleCount(), sampleRate);
    }

    long getExpectedSampleCount() {
        return sampleRate.asSampleCount((System.nanoTime()- timeZero) / 1000000000.);
    }

    public void addNote(double frequency) {
        noteManager.addNote(frequency);
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);

        return (long) ((frameTime*sampleLookahead - timePassed)/ 1000000);
    }
}
