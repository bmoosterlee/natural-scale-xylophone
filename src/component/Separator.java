package component;

import component.buffer.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Separator<T, L extends Collection<T>> extends AbstractComponent<L, T> {

    private final InputPort<L> inputPort;
    private final OutputPort<T> outputPort;

    public Separator(BoundedBuffer<L> inputBuffer, SimpleBuffer<T> outputBuffer) {
        inputPort = inputBuffer.createInputPort();
        outputPort = outputBuffer.createOutputPort();
    }

    protected void tick() {
        try {
            L input = inputPort.consume();
            for(T element : input){
                outputPort.produce(element);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T, L extends Collection<T>> SimpleBuffer<T> separate(BoundedBuffer<L> inputBuffer){
        SimpleBuffer<T> outputBuffer = new SimpleBuffer<>(1, "toBuffer - output");
        new TickRunningStrategy(new Separator<>(inputBuffer, outputBuffer));
        return outputBuffer;
    }

    @Override
    protected Collection<BoundedBuffer<L>> getInputBuffers() {
        return Collections.singleton(inputPort.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer<T>> getOutputBuffers() {
        return Collections.singleton(outputPort.getBuffer());
    }

    @Override
    public Boolean isParallelisable(){
        return false;
    }

}
