package component.buffer;

public class ChainedPipeComponent<K, V> extends MethodPipeComponent<K, V> {

    final ChainedPipeComponent previousComponent;

    public ChainedPipeComponent(BufferChainLink<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method){
        super(inputBuffer, outputBuffer, method);

        previousComponent = inputBuffer.previousComponent;
    }

    public ChainedPipeComponent(SimpleBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method){
        super(inputBuffer, outputBuffer, method);

        previousComponent = null;
    }

    public void tick() {
        try {
            previousComponent.tick();
        }
        catch(NullPointerException ignored){
        }

        super.tick();
    }

    private <T> InputPort<T> getFirstInputPort() {
        ChainedPipeComponent index = this;
        while(index.previousComponent!=null){
            index = index.previousComponent;
        }
        return index.input;
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        ChainedPipeComponent<K, V> methodComponent = new ChainedPipeComponent<>(inputBuffer, outputBuffer, method);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(SimpleBuffer<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        ChainedPipeComponent<K, V> methodComponent = new ChainedPipeComponent<>(inputBuffer, outputBuffer, method);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(BufferChainLink<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        ChainedPipeComponent<K, K> methodComponent = new ChainedPipeComponent<>(inputBuffer, outputBuffer, input -> input);
        BufferChainLink<K> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(SimpleBuffer<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        ChainedPipeComponent<K, K> methodComponent = new ChainedPipeComponent<>(inputBuffer, outputBuffer, input -> input);
        BufferChainLink<K> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

    public <T> AbstractPipeComponent<T, V> wrap() {
        return new AbstractPipeComponent<>(getFirstInputPort(), output) {

            @Override
            protected void tick() {
                ChainedPipeComponent.this.tick();
            }
        };
    }
}
