package sound;

import time.Ticker;

public class SampleTicker extends Ticker {

    private final int tickLookahead;
    private int maxBacklog;
    private final SampleRate sampleRate;

    public SampleTicker(SampleRate sampleRate){
        super(1000000000 / sampleRate.sampleRate);
        this.sampleRate = sampleRate;

        tickLookahead = sampleRate.sampleRate /1000;
        maxBacklog = sampleRate.sampleRate;
    }

    public void start(){
        new Thread(this).start();
    }

    @Override
    protected void tick() {
        long sampleBacklog = getExpectedTickCount() + tickLookahead - calculatedTicks;
        sampleBacklog = Math.min(sampleBacklog, maxBacklog);

        //todo create Milisecond datatype, nanoseconds datatype, doubleTime datatype, and a sampleCount datatype
        while(sampleBacklog>0) {
            super.tick();
            sampleBacklog--;
        }
    }

    public long getExpectedTickCount() {
        return sampleRate.asSampleCount((System.nanoTime()- timeZero) / 1000000000.);
    }

}
