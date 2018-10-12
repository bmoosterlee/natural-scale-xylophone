package time;

import main.Observable;

public class Ticker implements Runnable{

    private final Observable<Long> observable = new Observable<>();
    protected long calculatedTicks = 0L;
    private TimeInNanoSeconds timeZero;
    private final TimeInNanoSeconds frameTime;

    public Ticker(TimeInNanoSeconds frameTime){
        this.frameTime = frameTime;
    }

    public void start(){
        new Thread(this).start();
    }

    @Override
    public void run() {
        timeZero = TimeInNanoSeconds.now();

        while(true) {
            TimeInNanoSeconds startTime = TimeInNanoSeconds.now();
            
            tick();

            long timeLeftInFrame = getTimeLeftInFrame(startTime).toMilliSeconds().getValue();
            if(timeLeftInFrame>0){
                try {
                    Thread.sleep(timeLeftInFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void tick() {
        observable.notify(calculatedTicks);
        calculatedTicks++;
    }

    public long getExpectedTickCount() {
        return TimeInNanoSeconds.now().subtract(timeZero).divide(frameTime);
    }

    public TimeInNanoSeconds getTimeLeftInFrame(TimeInNanoSeconds startTime) {
        TimeInNanoSeconds currentTime = TimeInNanoSeconds.now();
        TimeInNanoSeconds timePassed = currentTime.subtract(startTime);
        return frameTime.subtract(timePassed);
    }

    public Observable<Long> getTickObservable() {
        return observable;
    }
}
