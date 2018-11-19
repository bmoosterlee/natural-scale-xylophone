package component;

import component.buffer.*;

public class TimedConsumer<T> extends MethodPipeComponent<Pulse, T> {

    public TimedConsumer(SimpleBuffer<Pulse> timeBuffer, BoundedBuffer<T> inputBuffer, SimpleBuffer<T> outputBuffer){
        super(timeBuffer, outputBuffer, consumeFrom(inputBuffer));
    }

    public static <T> CallableWithArguments<Pulse, T> consumeFrom(BoundedBuffer<T> inputBuffer){
        return new CallableWithArguments<>() {
            final InputPort<T> inputPort;

            {
                inputPort = inputBuffer.createInputPort();
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
