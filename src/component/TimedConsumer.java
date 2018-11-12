package component;

import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArguments;
import component.buffer.InputPort;
import component.buffer.SimpleBuffer;
import component.utilities.RunningPipeComponent;

public class TimedConsumer<T> extends RunningPipeComponent<Pulse, T> {

    public TimedConsumer(BoundedBuffer<Pulse> timeBuffer, BoundedBuffer<T> inputBuffer, SimpleBuffer<T> outputBuffer){
        super(timeBuffer, outputBuffer, consumeFrom(inputBuffer));
    }

    public static <T> CallableWithArguments<Pulse, T> consumeFrom(BoundedBuffer<T> inputBuffer){
        return new CallableWithArguments<>() {
            final InputPort<T> inputPort;

            {
                inputPort = new InputPort<>(inputBuffer);
            }


            private T consume() {
                try {
                    return inputPort.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public T call(Pulse input) {
                return consume();
            }
        };
    }

}
