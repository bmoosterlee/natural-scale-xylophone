package component;

import java.util.List;

public class Flusher<T> extends TickablePipeComponent {

    public Flusher(BoundedBuffer<Pulse> timeInputBuffer, BoundedBuffer<T> inputBuffer, BoundedBuffer<List<T>> outputBuffer) {
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

    public static <T> BoundedBuffer<List<T>> flush(BoundedBuffer<Pulse> timeInputBuffer, BoundedBuffer<T> inputBuffer){
        BoundedBuffer<List<T>> outputBuffer = new BoundedBuffer<>(1, "flush - output");
        new Flusher<>(timeInputBuffer, inputBuffer, outputBuffer);
        return outputBuffer;
    }

}
