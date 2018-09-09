package notes;

import javafx.util.Pair;
import main.PerformanceTracker;
import main.SampleRate;
import main.TimeKeeper;
import notes.envelope.SimpleDeterministicEnvelope;
import notes.envelope.functions.LinearFunctionMemoizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.*;

public class NoteEnvironment implements Runnable{

    private final int SAMPLE_SIZE_IN_BITS;
    private final SampleRate sampleRate;
    public final NoteManager noteManager;
    private SourceDataLine sourceDataLine;
    private long calculatedSamples = 0L;
    private long timeZero;
    private int sampleSize;
    private double marginalSampleSize;
    private long frameTime;
    private int sampleLookahead;
    LinkedList<Pair<Long, Set<Note>>> futureInaudibleNotes = new LinkedList<>();
    Long nextInaudibleNoteClearing = 0L;

    public NoteEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE){
        this.SAMPLE_SIZE_IN_BITS = SAMPLE_SIZE_IN_BITS;
        sampleRate = new SampleRate(SAMPLE_RATE);
        noteManager = new NoteManager(this);

        sampleSize = (int)(Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

        frameTime = 1000000000 / SAMPLE_RATE;
        sampleLookahead = SAMPLE_RATE/100;

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
        timeZero = System.nanoTime();
        long sampleBacklog;

        while(true) {
            long startTime = System.nanoTime();

            TimeKeeper removeInaudibleNotesTimeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment removeInaudibleNotes");
            try {
                while (getExpectedSampleCount() >= nextInaudibleNoteClearing) {
                    noteManager.removeInaudibleNotes(futureInaudibleNotes.pollFirst().getValue());
                    nextInaudibleNoteClearing = futureInaudibleNotes.peekFirst().getKey();
                }
            }
            catch(NullPointerException e){

            }
            catch(NoSuchElementException e){

            }
            PerformanceTracker.stopTracking(removeInaudibleNotesTimeKeeper);

            TimeKeeper tickTimeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick");
            sampleBacklog = getExpectedSampleCount() + sampleLookahead - calculatedSamples;
            sampleBacklog = Math.min(sampleBacklog, sampleRate.sampleRate);

            while(sampleBacklog>0) {
                tick();
                sampleBacklog--;
            }
            PerformanceTracker.stopTracking(tickTimeKeeper);

            TimeKeeper sleepTimeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment sleep");
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
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick getLiveNotes");
        NoteFrequencySnapshot noteFrequencySnapshot = noteManager.getSnapshot();
        NoteSnapshot noteSnapshot = noteFrequencySnapshot.noteSnapshot;
        FrequencySnapshot frequencySnapshot = noteFrequencySnapshot.frequencySnapshot;
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick getVolumeTable");
        HashMap<Note, Double> volumeTable = noteSnapshot.getVolumeTable(calculatedSamples);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick findInaudibleNotes");
        removeInaudibleNotes(noteSnapshot.liveNotes,
                             volumeTable);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick calculateAmplitudes");
        byte[] clipBuffer = new byte[]{calculateAmplitudeSum(sampleRate.asTime(calculatedSamples),
                                                             frequencySnapshot.liveFrequencies,
                                                             noteFrequencySnapshot.getFrequencyVolumeTable(volumeTable),
                                                             frequencySnapshot.frequencyAngleComponents)};
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick writeToBuffer");
        sourceDataLine.write(clipBuffer, 0, 1);
        calculatedSamples++;
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private void removeInaudibleNotes(HashSet<Note> liveNotes, HashMap<Note, Double> volumeTable) {
        Set<Note> inaudibleNotes = getInaudibleNotes(volumeTable, liveNotes);
        if(!inaudibleNotes.isEmpty()) {
            boolean hasInaudibleNotes = true;
            if(futureInaudibleNotes.isEmpty()){
                hasInaudibleNotes = false;
            }
            futureInaudibleNotes.add(new Pair<>(calculatedSamples, inaudibleNotes));
            if(!hasInaudibleNotes) {
                nextInaudibleNoteClearing = calculatedSamples;
            }
        }
    }

    private byte calculateAmplitudeSum(double time, Set<Frequency> liveFrequencies, Map<Frequency, Double> frequencyVolumeTable, HashMap<Frequency, Double> frequenciesAngleComponents) {
        double amplitudeSum = 0;
        for(Frequency frequency : liveFrequencies){
            amplitudeSum += getAmplitude(time,
                                         frequencyVolumeTable.get(frequency),
                                         frequenciesAngleComponents.get(frequency));
        }
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, (byte) Math.floor(sampleSize * amplitudeSum / 2)));
    }

    boolean isAudible(Double volume) {
        return volume >= marginalSampleSize;
    }

    private void initialize() {
        AudioFormat af = new AudioFormat((float) sampleRate.sampleRate, SAMPLE_SIZE_IN_BITS, 1, true, false);
        sourceDataLine = null;
        try {
            sourceDataLine = AudioSystem.getSourceDataLine(af);
            sourceDataLine.open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        sourceDataLine.start();
    }

    private double getAmplitude(double time, double volume, double frequencyAngleComponent) {
        double angle = time * frequencyAngleComponent;
        return (Math.sin(angle) * volume);
    }

    Note createNote(Frequency frequency) {
        return new Note(frequency, new SimpleDeterministicEnvelope(getExpectedSampleCount(), sampleRate, LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.05, 0.5)));
    }

    public long getExpectedSampleCount() {
        return sampleRate.asSampleCount((System.nanoTime()- timeZero) / 1000000000.);
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);
        return (frameTime - timePassed)/ 1000000;
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
