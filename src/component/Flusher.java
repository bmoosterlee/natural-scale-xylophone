package component;

import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArguments;
import component.buffer.SimpleBuffer;
import component.buffer.RunningPipeComponent;

import java.util.List;

public class Flusher<T> extends RunningPipeComponent<Pulse, List<T>> {

    public Flusher(BoundedBuffer<Pulse> timeInputBuffer, BoundedBuffer<T> inputBuffer, SimpleBuffer<List<T>> outputBuffer) {
        super(timeInputBuffer, outputBuffer, flush(inputBuffer));
    }

    public static <T> CallableWithArguments<Pulse, List<T>> flush(BoundedBuffer<T> inputBuffer){
        return input -> {
            try {
                return inputBuffer.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        };
    }

}
