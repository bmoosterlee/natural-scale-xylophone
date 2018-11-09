package time;

import component.BoundedBuffer;
import component.Component;
import component.OutputPort;

public class Ticker extends Component {

    private long calculatedTicks = 0L;
    private final TimeInNanoSeconds frameTime;

    private final OutputPort<Long> outputPort;

    public Ticker(BoundedBuffer<Long> outputBuffer, TimeInNanoSeconds frameTime){
        this.frameTime = frameTime;

        outputPort = new OutputPort<>(outputBuffer);

        start();
    }

    @Override
    public void run() {
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

    @Override
    protected void tick() {
        try {
            outputPort.produce(calculatedTicks);
            calculatedTicks++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private TimeInNanoSeconds getTimeLeftInFrame(TimeInNanoSeconds startTime) {
        TimeInNanoSeconds currentTime = TimeInNanoSeconds.now();
        TimeInNanoSeconds timePassed = currentTime.subtract(startTime);
        return frameTime.subtract(timePassed);
    }

}
