package component;

import component.buffer.*;

import java.util.*;

public class Collator<T> extends AbstractComponent<T, List<T>> {

    private final List<InputPort<T>> inputPorts;
    private final OutputPort<List<T>> outputPort;

    public Collator(List<BoundedBuffer<T>> inputBuffers, SimpleBuffer<List<T>> outputBuffer) {
        inputPorts = new ArrayList<>();
        for(BoundedBuffer<T> inputBuffer : inputBuffers){
            inputPorts.add(inputBuffer.createInputPort());
        }
        outputPort = outputBuffer.createOutputPort();
    }

    protected void tick() {
        try {
            final List<T> accumulator = new ArrayList<>();

            for(InputPort<T> input : inputPorts){
                accumulator.add(input.consume());
            }

            outputPort.produce(accumulator);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T> SimpleBuffer<List<T>> collate(List<BoundedBuffer<T>> inputBuffers){
        SimpleBuffer<List<T>> outputBuffer = new SimpleBuffer<>(1, "toBuffer - output");
        new TickRunningStrategy(new Collator<>(inputBuffers, outputBuffer));
        return outputBuffer;
    }

    @Override
    protected Collection<BoundedBuffer<T>> getInputBuffers() {
        List<BoundedBuffer<T>> inputBuffers = new ArrayList<>();
        for(InputPort<T> inputPort : inputPorts) {
            inputBuffers.add(inputPort.getBuffer());
        }
        return inputBuffers;
    }

    @Override
    protected Collection<BoundedBuffer<List<T>>> getOutputBuffers() {
        return Collections.singleton(outputPort.getBuffer());
    }

    @Override
    public Boolean isParallelisable(){
        return false;
    }

}
