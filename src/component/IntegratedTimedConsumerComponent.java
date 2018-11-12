package component;

import component.buffers.BoundedBuffer;
import component.buffers.CallableWithArguments;
import component.buffers.InputPort;
import component.buffers.SimpleBuffer;
import component.utilities.TickablePipeComponent;

public class IntegratedTimedConsumerComponent<T> extends TickablePipeComponent<Pulse, T> {

    public IntegratedTimedConsumerComponent(BoundedBuffer<Pulse> timeBuffer, BoundedBuffer<T> inputBuffer, SimpleBuffer<T> outputBuffer){
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
