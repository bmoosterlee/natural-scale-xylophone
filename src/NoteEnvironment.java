import javafx.util.Pair;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.*;

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
    LinkedList<Pair<Long, Set<Note>>> futureInaudibleNotes = new LinkedList<>();

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

            try {
                while (getExpectedSampleCount() >= futureInaudibleNotes.peekFirst().getKey()) {
                    noteManager.removeInaudibleNotes(futureInaudibleNotes.pollFirst().getValue());
                }
            }
            catch(NullPointerException e){

            }

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
        NoteFrequencySnapshot noteFrequencySnapshot = noteManager.getSnapshot();
        NoteSnapshot noteSnapshot = noteFrequencySnapshot.noteSnapshot;
        HashSet<Note> liveNotes = noteSnapshot.liveNotes;
        HashMap<Note, Envelope> envelopes = noteSnapshot.envelopes;
        FrequencySnapshot frequencySnapshot = noteFrequencySnapshot.frequencySnapshot;
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("NoteEnvironment getVolumeTable");
        HashMap<Note, Double> volumeTable = noteManager.getVolumeTable(calculatedSamples, liveNotes, envelopes);
        PerformanceTracker.stopTracking(timeKeeper);

        removeInaudibleNotes(liveNotes, volumeTable);

        timeKeeper = PerformanceTracker.startTracking("NoteEnvironment calculateAmplitudes");
        Set<Pair<Double, Double>> frequencyVolumes = getFrequencyVolumes(volumeTable, frequencySnapshot.frequencyNoteTable);
        byte[] clipBuffer = new byte[]{calculateAmplitudeSum(calculatedSamples, frequencyVolumes, frequencySnapshot.frequencyAngleComponents)};
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("NoteEnvironment writeToBuffer");
        getSourceDataLine().write(clipBuffer, 0, 1);
        calculatedSamples++;
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private void removeInaudibleNotes(HashSet<Note> liveNotes, HashMap<Note, Double> volumeTable) {
        TimeKeeper removeInaudibleNotesTimeKeeper = PerformanceTracker.startTracking("NoteEnvironment getInaudibleNotes");
        Set<Note> inaudibleNotes = getInaudibleNotes(volumeTable, liveNotes);
        if(!inaudibleNotes.isEmpty()) {
            futureInaudibleNotes.add(new Pair<>(calculatedSamples, inaudibleNotes));
        }
        PerformanceTracker.stopTracking(removeInaudibleNotesTimeKeeper);
    }

    private byte calculateAmplitudeSum(long calculatedSamples, Set<Pair<Double, Double>> frequencyVolumes, HashMap<Double, Double> frequenciesAngleComponents) {
        int amplitudeSum = 0;
        for(Pair<Double, Double> pair : frequencyVolumes){
            amplitudeSum += getAmplitude(calculatedSamples, pair.getValue(), frequenciesAngleComponents.get(pair.getKey()));
        }
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, amplitudeSum));
    }

    private Set<Pair<Double, Double>> getFrequencyVolumes(HashMap<Note, Double> volumeTable, Map<Double, Set<Note>> frequencyNoteTable) {
        Set<Pair<Double, Double>> frequencyVolumes = new HashSet<>();

        Iterator<Map.Entry<Double, Set<Note>>> iterator = frequencyNoteTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Double, Set<Note>> entry = iterator.next();
            Double frequency = entry.getKey();
            Double volume = 0.;
            Iterator<Note> noteIterator = entry.getValue().iterator();
            while (noteIterator.hasNext()) {
                Note note = noteIterator.next();
                try {
                    volume += volumeTable.get(note);
                } catch (NullPointerException e) {
                    continue;
                }
            }
            frequencyVolumes.add(new Pair<>(frequency, volume));
        }
        return frequencyVolumes;
    }

    boolean isAudible(Double volume) {
        return volume >= marginalSampleSize;
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

    private byte getAmplitude(long calculatedSamples, double volume, double frequencyAngleComponent) {
        double angle = sampleRate.asTime(calculatedSamples) * frequencyAngleComponent;
        double amplitude = (Math.sin(angle) * volume);
        return (byte) Math.floor(sampleSize * amplitude / 2);
    }

    private SourceDataLine getSourceDataLine() {
        return sourceDataLine;
    }

    private void setSourceDataLine(SourceDataLine sourceDataLine) {
        this.sourceDataLine = sourceDataLine;
    }

    Pair<Note, Envelope> createNote(double frequency) {
        return new Pair<>(new Note(frequency), new Envelope(getExpectedSampleCount(), sampleRate));
    }

    long getExpectedSampleCount() {
        return sampleRate.asSampleCount((System.nanoTime()- timeZero) / 1000000000.);
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);

        return (long) ((frameTime*sampleLookahead - timePassed)/ 1000000);
    }

    HashSet<Note> getInaudibleNotes(HashMap<Note, Double> volumeTable, HashSet<Note> liveNotes) {
        HashSet<Note> notesToBeRemoved = new HashSet<>();
        for (Note note : liveNotes) {
            if (!isAudible(volumeTable.get(note))) {
                notesToBeRemoved.add(note);
            }
        }
        return notesToBeRemoved;
    }
}
