package component;

import component.buffer.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Unzipper<T> extends AbstractComponent<List<T>, T> {

    private final InputPort<List<T>> inputPort;
    private final OutputPort<T> outputPort;

    public Unzipper(BoundedBuffer<List<T>> inputBuffer, SimpleBuffer<T> outputBuffer) {
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

    public static <T> SimpleBuffer<T> unzip(BoundedBuffer<List<T>> inputBuffer){
        SimpleBuffer<T> outputBuffer = new SimpleBuffer<>(1, "toBuffer - output");
        new TickRunningStrategy(new Unzipper<>(inputBuffer, outputBuffer));
        return outputBuffer;
    }

    @Override
    protected Collection<InputPort<List<T>>> getInputPorts() {
        return Collections.singleton(inputPort);
    }

    @Override
    protected Collection<OutputPort<T>> getOutputPorts() {
        return Collections.singleton(outputPort);
    }

}
