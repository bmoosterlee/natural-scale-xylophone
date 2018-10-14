package sound;

import time.Ticker;
import time.TimeInNanoSeconds;

public class SampleTicker extends Ticker {

    private final int maxBacklog;

    public SampleTicker(TimeInNanoSeconds frameTime, int maxBacklog){
        super(frameTime);

        this.maxBacklog = maxBacklog;
    }

    public void start(){
        new Thread(this).start();
    }

    @Override
    protected void tick() {
        long sampleBacklog = getExpectedTickCount() - calculatedTicks;
        sampleBacklog = Math.min(sampleBacklog, maxBacklog);

        while(sampleBacklog>0) {
            super.tick();
            sampleBacklog--;
        }
    }

}
