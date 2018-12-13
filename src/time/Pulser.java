package time;

import component.Pulse;
import component.buffer.MethodOutputComponent;
import component.buffer.SimpleBuffer;
import component.buffer.OutputCallable;
import component.buffer.SimplePacket;

public class Pulser extends MethodOutputComponent<Pulse> {

    private static final Pulse pulse = new Pulse();

    public Pulser(SimpleBuffer<Pulse, SimplePacket<Pulse>> outputBuffer, TimeInNanoSeconds frameTime){
        super(outputBuffer, build(frameTime));
    }

    public static OutputCallable<Pulse> build(TimeInNanoSeconds frameTime) {
        return new OutputCallable<>() {
            TimeInNanoSeconds startTime = TimeInNanoSeconds.now();
            long frameCount = 0;

            @Override
            public Pulse call() {
                TimeInNanoSeconds endTime = startTime.add(frameTime.multiply(frameCount));
                TimeInNanoSeconds currentTime = TimeInNanoSeconds.now();
                long timeLeftInFrame = endTime.subtract(currentTime).toMilliSeconds().getValue();
                if (timeLeftInFrame > 0) {
                    try {
                        Thread.sleep(timeLeftInFrame);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                frameCount = frameCount + 1;
                return pulse;
            }

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }

}
