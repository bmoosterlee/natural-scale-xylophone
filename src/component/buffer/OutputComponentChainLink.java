package component.buffer;

import main.OutputCallable;

public class OutputComponentChainLink<V> extends ComponentChainLink {
    private final MethodOutputComponent methodOutputComponent;

    public OutputComponentChainLink(MethodOutputComponent<V> methodOutputComponent) {
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
    InputPort getFirstInputPort() {
        return null;
    }

    @Override
    Boolean isParallelisable() {
        return methodOutputComponent.isParallelisable();
    }

    @Override
    protected OutputPort getOutputPort() {
        return methodOutputComponent.output;
    }

    protected void parallelisationAwareTick() {
        componentTick();
    }

    @Override
    InputPort getParallelisationAwareFirstInputPort() {
        return null;
    }

    @Override
    public AbstractComponent wrap() {
        parallelChainCheck(this);

        return new AbstractOutputComponent<>(getOutputPort()) {

            @Override
            protected void tick() {
                OutputComponentChainLink.this.tick();
            }

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }

    public static <V> BufferChainLink<V> buildOutputBuffer(OutputCallable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        MethodOutputComponent<V> component = new MethodOutputComponent<>(outputBuffer, method);
        OutputComponentChainLink<V> componentChainLink = new OutputComponentChainLink<>(component);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }
}
