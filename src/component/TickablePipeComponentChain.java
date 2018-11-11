package component;

public class TickablePipeComponentChain<K, V> extends TickablePipeComponent{

    private final TickablePipeComponentChain previousComponent;

    public TickablePipeComponentChain(BufferChainLink<K> inputBuffer, BufferInterface<V> outputBuffer, CallableWithArguments<K, V> method){
        super(inputBuffer, outputBuffer, method);

        previousComponent = inputBuffer.previousComponent;
    }

    protected void tick() {
        try {
            previousComponent.tick();
        }
        catch(NullPointerException ignored){
        }

        super.tick();
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        BoundedBuffer<V> outputBuffer = new BoundedBuffer<>(capacity, name);
        TickablePipeComponentChain<K, V> methodComponent = new TickablePipeComponentChain<>(inputBuffer, outputBuffer, method);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

}
