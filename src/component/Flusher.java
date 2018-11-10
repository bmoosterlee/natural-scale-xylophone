package component;

import java.util.Collection;
import java.util.List;

public class Flusher<T> extends TimedConsumerComponent {

    private final InputPort<T> inputPort;
    private final OutputPort<Collection<T>> outputPort;

    public Flusher(BoundedBuffer<Pulse> timeInputBuffer, BoundedBuffer<T> inputBuffer, BoundedBuffer<Collection<T>> outputBuffer) {
        super(timeInputBuffer);
        inputPort = inputBuffer.createInputPort();
        outputPort = outputBuffer.createOutputPort();
    }

    @Override
    protected void timedTick() {
        try {
            List<T> input = inputPort.flush();
            outputPort.produce(input);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static <T> BoundedBuffer<Collection<T>> flush(BoundedBuffer<Pulse> timeInputBuffer, BoundedBuffer<T> inputBuffer){
        BoundedBuffer<Collection<T>> outputBuffer = new BoundedBuffer<>(1, "flush - output");
        new Flusher<>(timeInputBuffer, inputBuffer, outputBuffer);
        return outputBuffer;
    }

}
