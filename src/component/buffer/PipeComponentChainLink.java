package component.buffer;

public class PipeComponentChainLink<K, V> extends ComponentChainLink {
    private final MethodPipeComponent<K, V> methodPipeComponent;

    public PipeComponentChainLink(ComponentChainLink previousComponentChainLink, MethodPipeComponent<K, V> methodPipeComponent){
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

    @Override
    InputPort getFirstInputPort() {
        try{
            InputPort firstInputPort = previousComponentChainLink.getFirstInputPort();
            if(firstInputPort!=null) {
                return firstInputPort;
            }
        }
        catch(NullPointerException ignored) {
        }
        return getInputPort();
    }

    @Override
    protected InputPort<K> getInputPort() {
        return methodPipeComponent.input;
    }

    @Override
    protected OutputPort getOutputPort() {
        return methodPipeComponent.output;
    }

    @Override
    Boolean isParallelisable() {
        return methodPipeComponent.isParallelisable();
    }

    @Override
    protected AbstractComponent parallelWrap() {
        return new AbstractPipeComponent<>(getParallelisationAwareFirstInputPort(), getOutputPort()) {
            @Override
            protected void tick() {
                parallelisationAwareTick();
            }
        };
    }

    @Override
    public AbstractPipeComponent wrap() {
        tryToBreakParallelChain();

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

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, PipeCallable<K,V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        MethodPipeComponent<K, V> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, method);
        tryToBreakParallelChain(inputBuffer, component);
        return getBufferChainLink(inputBuffer, outputBuffer, component);
    }

    private static <K, V> BufferChainLink<V> getBufferChainLink(BufferChainLink<K> inputBuffer, SimpleBuffer<V> outputBuffer, MethodPipeComponent<K, V> component) {
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<K, V>(inputBuffer.previousComponent, component);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    public static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(SimpleBuffer<K> inputBuffer, PipeCallable<K,V> method, int capacity, String name) {
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
        tryToBreakParallelChain(inputBuffer, component);
        return getBufferChainLink(inputBuffer, outputBuffer, component);
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(SimpleBuffer<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        MethodPipeComponent<K, K> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input);
        return getBufferChainLink(outputBuffer, component);
    }
}
