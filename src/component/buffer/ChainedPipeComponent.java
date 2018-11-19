package component.buffer;

public class ChainedPipeComponent<K, V> {
    final MethodPipeComponent<K, V> methodPipeComponent;
    final ChainedPipeComponent previousComponent;

    public ChainedPipeComponent(ChainedPipeComponent<?, K> previousComponent, MethodPipeComponent<K, V> methodPipeComponent){
        this.methodPipeComponent = methodPipeComponent;

        this.previousComponent = previousComponent;
    }

    public ChainedPipeComponent(MethodPipeComponent<K, V> methodPipeComponent){
        this(null, methodPipeComponent);
    }

    protected void tick() {
        try {
            previousComponent.tick();
        }
        catch(NullPointerException ignored){
        }

        methodPipeComponent.tick();
    }

    private <T> InputPort<T> getFirstInputPort() {
        ChainedPipeComponent index = this;
        while(index.previousComponent!=null){
            index = index.previousComponent;
        }
        return index.methodPipeComponent.input;
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        ChainedPipeComponent<K, V> methodComponent = new ChainedPipeComponent<>(inputBuffer.previousComponent, new MethodPipeComponent<>(inputBuffer, outputBuffer, method));
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(SimpleBuffer<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        ChainedPipeComponent<K, V> methodComponent = new ChainedPipeComponent<>(new MethodPipeComponent<>(inputBuffer, outputBuffer, method));
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(BufferChainLink<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        ChainedPipeComponent<K, K> methodComponent = new ChainedPipeComponent<>(inputBuffer.previousComponent, new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input));
        BufferChainLink<K> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(SimpleBuffer<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        ChainedPipeComponent<K, K> methodComponent = new ChainedPipeComponent<>(new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input));
        BufferChainLink<K> outputChainLink = new BufferChainLink<>(outputBuffer, methodComponent);
        return outputChainLink;
    }

    public <T> AbstractPipeComponent<T, V> wrap() {
        return new AbstractPipeComponent<>(getFirstInputPort(), methodPipeComponent.output) {

            @Override
            protected void tick() {
                ChainedPipeComponent.this.tick();
            }
        };
    }
}
