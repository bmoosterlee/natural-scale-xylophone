package component;

import java.util.List;

public class Flusher<T> extends TickablePipeComponent<Pulse, List<T>> {

    public Flusher(BufferInterface<Pulse> timeInputBuffer, BufferInterface<T> inputBuffer, SimpleBuffer<List<T>> outputBuffer) {
        super(timeInputBuffer, outputBuffer, flush(inputBuffer));
    }

    public static <T> CallableWithArguments<Pulse, List<T>> flush(BufferInterface<T> inputBuffer){
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
