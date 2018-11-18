package component;

import component.buffer.*;

import java.util.List;

public class Flusher<T> extends PipeComponent<Pulse, List<T>> {

    public Flusher(SimpleBuffer<Pulse> timeInputBuffer, BoundedBuffer<T> inputBuffer, SimpleBuffer<List<T>> outputBuffer) {
        super(timeInputBuffer, outputBuffer, flush(inputBuffer));
    }

    public static <T> CallableWithArguments<Pulse, List<T>> flush(BoundedBuffer<T> inputBuffer){
        InputPort<T> inputPort = inputBuffer.createInputPort();

        return input -> {
            try {
                return inputPort.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        };
    }

}
