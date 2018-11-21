package component.buffer;

public class PipeComponentChainLink<K, V> extends ComponentChainLink<K> {
    private final MethodPipeComponent<K, V> methodPipeComponent;

    public PipeComponentChainLink(PipeComponentChainLink<?, K> previousComponentChainLink, MethodPipeComponent<K, V> methodPipeComponent){
        super(previousComponentChainLink);
        this.methodPipeComponent = methodPipeComponent;

    }

    public PipeComponentChainLink(MethodPipeComponent<K, V> methodPipeComponent){
        this(null, methodPipeComponent);
    }

    @Override
    protected void componentTick() {
        methodPipeComponent.tick();
    }

    public <T> AbstractPipeComponent<T, V> wrap() {
        return new AbstractPipeComponent<>(getFirstInputPort(), methodPipeComponent.output) {

            @Override
            protected void tick() {
                PipeComponentChainLink.this.tick();
            }
        };
    }

    InputPort getFirstInputPort() {
        try{
            return previousComponentChainLink.getFirstInputPort();
        }
        catch(NullPointerException e) {
            return methodPipeComponent.input;
        }
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<>(inputBuffer.previousComponent, new MethodPipeComponent<>(inputBuffer, outputBuffer, method));
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(SimpleBuffer<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<>(new MethodPipeComponent<>(inputBuffer, outputBuffer, method));
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(BufferChainLink<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        PipeComponentChainLink<K, K> componentChainLink = new PipeComponentChainLink<>(inputBuffer.previousComponent, new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input));
        BufferChainLink<K> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(SimpleBuffer<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        PipeComponentChainLink<K, K> componentChainLink = new PipeComponentChainLink<>(new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input));
        BufferChainLink<K> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }
}
