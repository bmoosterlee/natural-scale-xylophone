package time;

import component.Pulse;
import component.buffer.MethodOutputComponent;
import component.buffer.SimpleBuffer;
import component.buffer.SimpleTickRunner;

import java.util.concurrent.Callable;

public class Pulser {

    public Pulser(SimpleBuffer<Pulse> outputBuffer, TimeInNanoSeconds frameTime){
        final MethodOutputComponent<Pulse> outputComponent = new MethodOutputComponent<>(outputBuffer, build(frameTime));

        new SimpleTickRunner(){

            @Override
            protected void tick() {
                outputComponent.tick();
            }
        }.start();
    }

    private static Callable<Pulse> build(TimeInNanoSeconds frameTime) {
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

    public static SimpleBuffer<Pulse> buildOutputBuffer(TimeInNanoSeconds frameTime, int capacity, String name) {
        SimpleBuffer<Pulse> outputBuffer = new SimpleBuffer<>(capacity, name);
        new Pulser(outputBuffer, frameTime);
        return outputBuffer;
    }

}