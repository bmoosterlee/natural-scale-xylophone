package notes.state;

import main.PerformanceTracker;
import main.SampleRate;
import main.TimeKeeper;
import notes.Frequency;
import notes.Note;
import notes.envelope.PrecalculatedEnvelope;
import notes.envelope.SimpleDeterministicEnvelope;
import notes.envelope.functions.DeterministicFunction;
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
    private final DeterministicFunction envelopeFunction;

    public NoteEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE){
        this.SAMPLE_SIZE_IN_BITS = SAMPLE_SIZE_IN_BITS;
        sampleRate = new SampleRate(SAMPLE_RATE);
        noteManager = new NoteManager(this, sampleRate);

        sampleSize = (int)(Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

        frameTime = 1000000000 / SAMPLE_RATE;
        sampleLookahead = SAMPLE_RATE/100;

        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.05, 0.4);

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
        NoteSnapshot noteSnapshot = noteManager.getSnapshot(calculatedSamples);
        FrequencyState frequencyState = noteSnapshot.frequencyState;
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick calculateAmplitudes");
        // if(calculatedSamples%sampleRate.sampleRate==0) {
        //     System.out.println(noteState.notes.size());
        //     System.out.println(frequencyState.getFrequencies().size());
        // }
        byte[] clipBuffer = new byte[]{calculateAmplitudeSum(calculatedSamples,
                                                             frequencyState.getFrequencies(),
                                                             frequencyState,
                                                             noteSnapshot.waveState)};
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick writeToBuffer");
        sourceDataLine.write(clipBuffer, 0, 1);
        calculatedSamples++;
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private byte calculateAmplitudeSum(long sampleCount, Set<Frequency> liveFrequencies, FrequencyState frequencyState, WaveState waveState) {
        double amplitudeSum = 0;
        for(Frequency frequency : liveFrequencies){
            Double volume = frequencyState.getVolume(frequency, sampleCount);
            double amplitude = 0.;
            try {
                amplitude = waveState.getAmplitude(frequency, sampleCount);
            }
            catch(NullPointerException e){

            }
            amplitudeSum += volume * amplitude;
        }
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, (byte) Math.floor(sampleSize * amplitudeSum / 2)));
    }

    public boolean isAudible(Double volume) {
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

    Note createNote(Frequency frequency) {
        return new Note(frequency, new PrecalculatedEnvelope(new SimpleDeterministicEnvelope(getExpectedSampleCount(), sampleRate, envelopeFunction)));
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

}
