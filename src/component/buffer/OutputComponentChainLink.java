package component.buffer;

public class OutputComponentChainLink<V> extends ComponentChainLink {
    private final MethodOutputComponent methodOutputComponent;

    private OutputComponentChainLink(MethodOutputComponent<V> methodOutputComponent) {
        super(null);
        this.methodOutputComponent = methodOutputComponent;
    }

    @Override
    protected void componentTick() {
        methodOutputComponent.tick();
    }

    @Override
    protected AbstractComponent parallelWrap() {
        return new AbstractOutputComponent<>(getOutputPort()) {
            @Override
            protected void tick() {
                parallelisationAwareTick();
            }
        };
    }

    @Override
    Boolean isParallelisable() {
        return methodOutputComponent.isParallelisable();
    }

    @Override
    protected InputPort getInputPort() {
        return null;
    }

    @Override
    protected OutputPort getOutputPort() {
        return methodOutputComponent.output;
    }

    public static <V> BufferChainLink<V> buildOutputBuffer(OutputCallable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        MethodOutputComponent<V> component = new MethodOutputComponent<>(outputBuffer, method);
        OutputComponentChainLink<V> componentChainLink = new OutputComponentChainLink<>(component);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }
}
