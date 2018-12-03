package time;

import component.Pulse;
import component.buffer.MethodOutputComponent;
import component.buffer.SimpleBuffer;
import component.buffer.OutputCallable;
import component.buffer.SimplePacket;

public class Pulser extends MethodOutputComponent<Pulse> {

    public Pulser(SimpleBuffer<Pulse, SimplePacket<Pulse>> outputBuffer, TimeInNanoSeconds frameTime){
        super(outputBuffer, build(frameTime));
    }

    public static OutputCallable<Pulse> build(TimeInNanoSeconds frameTime) {
        return new OutputCallable<>() {

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

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }

}
