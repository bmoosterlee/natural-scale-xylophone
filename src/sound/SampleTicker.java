package sound;

import time.Ticker;
import time.TimeInSeconds;

public class SampleTicker extends Ticker {

    private final long tickLookahead;
    private int maxBacklog;

    public SampleTicker(SampleRate sampleRate){
        super(new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate));

        tickLookahead = sampleRate.asSampleCount(new TimeInSeconds(1).toNanoSeconds().divide(1000));
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

}
