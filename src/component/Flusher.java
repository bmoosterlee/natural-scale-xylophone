package component;

import component.buffer.*;

import java.util.List;

public class Flusher<T> extends MethodPipeComponent<Pulse, List<T>> {

    public Flusher(SimpleBuffer<Pulse> timeInputBuffer, BoundedBuffer<T> inputBuffer, SimpleBuffer<List<T>> outputBuffer) {
        super(timeInputBuffer, outputBuffer, flush(inputBuffer));
    }

    public static <T> PipeCallable<Pulse, List<T>> flush(BoundedBuffer<T> inputBuffer){
        InputPort<T> inputPort = inputBuffer.createInputPort();

        return new PipeCallable<>() {
            @Override
            public List<T> call(Pulse input) {
                try {
                    return inputPort.flush();
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
