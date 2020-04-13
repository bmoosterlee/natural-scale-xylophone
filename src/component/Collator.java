package component;

import component.buffer.*;

import java.util.*;

public class Collator<T, A extends Packet<T>> extends AbstractComponent<T, List<T>, A, SimplePacket<List<T>>> {

    private final List<InputPort<T, A>> inputPorts;
    private final OutputPort<List<T>, SimplePacket<List<T>>> outputPort;

    public Collator(List<BoundedBuffer<T, A>> inputBuffers, SimpleBuffer<List<T>, SimplePacket<List<T>>> outputBuffer) {
        inputPorts = new ArrayList<>();
        for(BoundedBuffer<T, A> inputBuffer : inputBuffers){
            inputPorts.add(inputBuffer.createInputPort());
        }
        outputPort = outputBuffer.createOutputPort();
    }

    protected void tick() {
        try {
            final List<T> accumulator = new ArrayList<>();

            for(InputPort<T, A> input : inputPorts){
                accumulator.add(input.consume().unwrap());
            }

            outputPort.produce(new SimplePacket<>(accumulator));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T, B extends Packet<T>> SimpleBuffer<List<T>, SimplePacket<List<T>>> collate(List<BoundedBuffer<T, B>> inputBuffers){
        SimpleBuffer<List<T>, SimplePacket<List<T>>> outputBuffer = new SimpleBuffer<>(1, "toBuffer - output");
        new TickRunningStrategy(new Collator<>(inputBuffers, outputBuffer));
        return outputBuffer;
    }

    @Override
    protected Collection<BoundedBuffer<T, A>> getInputBuffers() {
        List<BoundedBuffer<T, A>> inputBuffers = new ArrayList<>();
        for(InputPort<T, A> inputPort : inputPorts) {
            inputBuffers.add(inputPort.getBuffer());
        }
        return inputBuffers;
    }

    @Override
    protected Collection<BoundedBuffer<List<T>, SimplePacket<List<T>>>> getOutputBuffers() {
        return Collections.singleton(outputPort.getBuffer());
    }

}
