package component;

import component.buffer.*;

public class TimedConsumer {

    public static <K, A extends Packet<K>> PipeCallable<Pulse, K> consumeFrom(BoundedBuffer<K, A> inputBuffer){
        return new PipeCallable<>() {
            final InputPort<K, A> inputPort;

            {
                inputPort = inputBuffer.createInputPort();
            }


            @Override
            public K call(Pulse input) {
                return consume().unwrap();
            }

            private A consume() {
                try {
                    return inputPort.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

}
