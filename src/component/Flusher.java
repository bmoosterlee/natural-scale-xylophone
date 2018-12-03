package component;

import component.buffer.*;

import java.util.List;
import java.util.stream.Collectors;

public class Flusher<T, A extends Packet<Pulse>, B extends Packet<T>, C extends Packet<List<T>>> extends MethodPipeComponent<Pulse, List<T>, A, C> {

    public Flusher(SimpleBuffer<Pulse, A> timeInputBuffer, BoundedBuffer<T, B> inputBuffer, SimpleBuffer<List<T>, C> outputBuffer) {
        super(timeInputBuffer, outputBuffer, flush(inputBuffer));
    }

    public static <T, B extends Packet<T>> PipeCallable<Pulse, List<T>> flush(BoundedBuffer<T, B> inputBuffer){
        InputPort<T, B> inputPort = inputBuffer.createInputPort();

        return new PipeCallable<>() {
            @Override
            public List<T> call(Pulse input) {
                try {
                    return inputPort.flush().stream().map(input1 -> input1.unwrap()).collect(Collectors.toList());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Boolean isParallelisable() {
                return false;
            }
        };
    }

}
