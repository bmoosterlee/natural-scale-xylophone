package component.utilities;

import component.buffer.BoundedBuffer;
import component.buffer.BufferChainLink;
import component.buffer.CallableWithArguments;
import component.buffer.SimpleBuffer;

public class PipeComponentChain<K, V> extends PipeComponent<K, V>{

    private final PipeComponentChain previousComponent;

    public PipeComponentChain(BufferChainLink<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method){
        super(inputBuffer, outputBuffer, method);

        previousComponent = inputBuffer.previousComponent;
    }

    public void tick() {
        try {
            previousComponent.tick();
        }
        catch(NullPointerException ignored){
        }

        super.tick();
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        PipeComponentChain<K, V> methodComponent = new PipeComponentChain<>(inputBuffer, outputBuffer, method);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

}
