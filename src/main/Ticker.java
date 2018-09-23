package main;

public class Ticker implements Runnable{

    private final Observable<Long> observable = new Observable<>();
    protected long calculatedTicks = 0L;
    protected long timeZero;
    private final long frameTime;

    public Ticker(long frameTime){
        this.frameTime = frameTime;
    }

    public void start(){
        new Thread(this).start();
    }

    @Override
    public void run() {
        timeZero = System.nanoTime();

        while(true) {
            long startTime = System.nanoTime();

            //todo create Milisecond datatype, nanoseconds datatype, doubleTime datatype, and a sampleCount datatype
            tick();

            long timeLeftInFrame = getTimeLeftInFrame(startTime);
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
        return (long) (((System.nanoTime()- timeZero) / 1000000000.)/frameTime);
    }

    public long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);
        return (frameTime - timePassed)/ 1000000;
    }

    public Observable getTickObservable() {
        return observable;
    }
}
