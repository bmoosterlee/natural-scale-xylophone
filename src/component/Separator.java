package component;

import component.buffer.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Separator<T, L extends Collection<T>, A extends Packet<T>, B extends Packet<L>> extends AbstractComponent<L, T, B, A> {

    private final InputPort<L, B> inputPort;
    private final OutputPort<T, A> outputPort;

    public Separator(BoundedBuffer<L, B> inputBuffer, SimpleBuffer<T, A> outputBuffer) {
        inputPort = inputBuffer.createInputPort();
        outputPort = outputBuffer.createOutputPort();
    }

    protected void tick() {
        try {
            B input = inputPort.consume();

            Collection<A> col = new HashSet<>();
            for(T element : input.unwrap()){
                col.add(input.transform(in -> element));
            }

            for(A element : col) {
                outputPort.produce(element);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T, L extends Collection<T>, A extends Packet<T>, B extends Packet<L>> SimpleBuffer<T, A> separate(BoundedBuffer<L, B> inputBuffer){
        SimpleBuffer<T, A> outputBuffer = new SimpleBuffer<>(1, "toBuffer - output");
        new TickRunningStrategy(new Separator<>(inputBuffer, outputBuffer));
        return outputBuffer;
    }

    @Override
    protected Collection<BoundedBuffer<L, B>> getInputBuffers() {
        return Collections.singleton(inputPort.getBuffer());
    }

    @Override
    protected Collection<BoundedBuffer<T, A>> getOutputBuffers() {
        return Collections.singleton(outputPort.getBuffer());
    }

    @Override
    public Boolean isParallelisable(){
        return false;
    }

}
