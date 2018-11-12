package time;

import component.*;
import component.buffers.SimpleBuffer;
import component.utilities.TickableOutputComponent;

import java.util.concurrent.Callable;

public class Ticker extends TickableOutputComponent<Pulse> {

    public Ticker(SimpleBuffer<Pulse> outputBuffer, TimeInNanoSeconds frameTime){
        super(outputBuffer, build(frameTime));
    }

    public static Callable<Pulse> build(TimeInNanoSeconds frameTime) {
        return new Callable<>() {

            private TimeInNanoSeconds getTimeLeftInFrame(TimeInNanoSeconds startTime) {
                TimeInNanoSeconds currentTime = TimeInNanoSeconds.now();
                TimeInNanoSeconds timePassed = currentTime.subtract(startTime);
                return frameTime.subtract(timePassed);
            }

            @Override
            public Pulse call() {
                TimeInNanoSeconds startTime = TimeInNanoSeconds.now();

                long timeLeftInFrame = getTimeLeftInFrame(startTime).toMilliSeconds().getValue();
                if (timeLeftInFrame > 0) {
                    try {
                        Thread.sleep(timeLeftInFrame);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                return new Pulse();
            }
        };
    }

}
