package component;

import component.buffer.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Separator<T> extends AbstractComponent<List<T>, T> {

    private final InputPort<List<T>> inputPort;
    private final OutputPort<T> outputPort;

    public Separator(BoundedBuffer<List<T>> inputBuffer, SimpleBuffer<T> outputBuffer) {
        inputPort = inputBuffer.createInputPort();
        outputPort = outputBuffer.createOutputPort();
    }

    protected void tick() {
        try {
            List<T> input = inputPort.consume();
            for(T element : input){
                outputPort.produce(element);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T> SimpleBuffer<T> separate(BoundedBuffer<List<T>> inputBuffer){
        SimpleBuffer<T> outputBuffer = new SimpleBuffer<>(1, "toBuffer - output");
        new TickRunningStrategy(new Separator<>(inputBuffer, outputBuffer));
        return outputBuffer;
    }

    @Override
    protected Collection<BoundedBuffer<List<T>>> getInputBuffers() {
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
