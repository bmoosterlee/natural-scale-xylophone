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

    protected void parallelisationAwareTick() {
        try{
            if(previousComponentChainLink.isParallelisable() == isParallelisable()) {
                previousComponentChainLink.tick();
            }
        }
        catch(NullPointerException ignored){
        }

        componentTick();
    }

    @Override
    protected void componentTick() {
        methodPipeComponent.tick();
    }

    @Override
    protected OutputPort getOutputPort() {
        return methodPipeComponent.output;
    }

    public <T> AbstractPipeComponent<T, V> wrap() {
        return new AbstractPipeComponent<>(getFirstInputPort(), getOutputPort()) {

            @Override
            protected void tick() {
                PipeComponentChainLink.this.tick();
            }

            @Override
            public Boolean isParallelisable(){
                return false;
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

    @Override
    Boolean isParallelisable() {
        return methodPipeComponent.isParallelisable();
    }

    InputPort getParallisationAwareFirstInputPort() {
        try{
            if(previousComponentChainLink.isParallelisable() == isParallelisable()) {
                return previousComponentChainLink.getFirstInputPort();
            }
        }
        catch(NullPointerException ignored) {
        }

        return methodPipeComponent.input;
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        MethodPipeComponent<K, V> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, method);
        parallelChainCheck(inputBuffer, component);
        return getBufferChainLink(inputBuffer, outputBuffer, component);
    }

    private static <K, V> BufferChainLink<V> getBufferChainLink(BufferChainLink<K> inputBuffer, SimpleBuffer<V> outputBuffer, MethodPipeComponent<K, V> component) {
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<>(inputBuffer.previousComponent, component);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(SimpleBuffer<K> inputBuffer, CallableWithArguments<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        MethodPipeComponent<K, V> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, method);
        return getBufferChainLink(outputBuffer, component);
    }

    private static <K, V> BufferChainLink<V> getBufferChainLink(SimpleBuffer<V> outputBuffer, MethodPipeComponent<K, V> component) {
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<>(component);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(BufferChainLink<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        MethodPipeComponent<K, K> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input);
        parallelChainCheck(inputBuffer, component);
        return getBufferChainLink(inputBuffer, outputBuffer, component);
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(SimpleBuffer<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        MethodPipeComponent<K, K> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input);
        return getBufferChainLink(outputBuffer, component);
    }
}
