package component;

public class IntegratedTimedConsumerComponent<T> extends TickablePipeComponent<Pulse, T> {

    public IntegratedTimedConsumerComponent(BoundedBuffer<Pulse> timeBuffer, BoundedBuffer<T> inputBuffer, BoundedBuffer<T> outputBuffer){
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
