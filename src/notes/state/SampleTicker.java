package notes.state;

import main.PerformanceTracker;
import main.TimeKeeper;

import java.util.Collection;
import java.util.HashSet;

public class SampleTicker implements Runnable{

    private final SoundEnvironment soundEnvironment;
    private long calculatedSamples = 0L;
    private long timeZero;
    private long frameTime;
    private int sampleLookahead;
    public Collection<Observer<Long>> tickObservers;

    public SampleTicker(SoundEnvironment soundEnvironment){
        this.soundEnvironment = soundEnvironment;
        tickObservers = new HashSet<>();

        int sampleRate = soundEnvironment.getSampleRate().sampleRate;
        frameTime = 1000000000 / sampleRate;
        sampleLookahead = sampleRate/100;
    }

    public void start(){
        new Thread(this).start();
    }

    @Override
    public void run() {
        timeZero = System.nanoTime();
        long sampleBacklog;

        while(true) {
            long startTime = System.nanoTime();

            TimeKeeper tickTimeKeeper = PerformanceTracker.startTracking("SampleTicker tick");
            sampleBacklog = getExpectedSampleCount() + sampleLookahead - calculatedSamples;
            sampleBacklog = Math.min(sampleBacklog, soundEnvironment.getSampleRate().sampleRate);

            while(sampleBacklog>0) {
                for(Observer observer : tickObservers) {
                    observer.notify(calculatedSamples);
                }
                calculatedSamples++;
                sampleBacklog--;
            }
            PerformanceTracker.stopTracking(tickTimeKeeper);

            TimeKeeper sleepTimeKeeper = PerformanceTracker.startTracking("SampleTicker sleep");
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

    public long getExpectedSampleCount() {
        return soundEnvironment.getSampleRate().asSampleCount((System.nanoTime()- timeZero) / 1000000000.);
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);
        return (frameTime - timePassed)/ 1000000;
    }

    public long getCalculatedSamples() {
        return calculatedSamples;
    }
}
