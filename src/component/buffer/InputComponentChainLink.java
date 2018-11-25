package component.buffer;

public class InputComponentChainLink<K> extends ComponentChainLink {
    private final MethodInputComponent<K> methodInputComponent;

    public InputComponentChainLink(ComponentChainLink previousComponentChainLink, MethodInputComponent<K> methodInputComponent){
        super(previousComponentChainLink);
        this.methodInputComponent = methodInputComponent;
    }

    @Override
    protected void componentTick() {
        methodInputComponent.tick();
    }

    @Override
    protected InputPort<K> getInputPort() {
        return methodInputComponent.input;
    }

    @Override
    protected OutputPort getOutputPort() {
        return null;
    }

    @Override
    Boolean isParallelisable() {
        return methodInputComponent.isParallelisable();
    }

    @Override
    protected AbstractComponent parallelWrap() {
        return new AbstractInputComponent<>(getParallelisationAwareFirstInputPort()) {
            @Override
            protected void tick() {
                parallelisationAwareTick();
            }
        };
    }

    @Override
    protected AbstractInputComponent sequentialWrap() {
        return new AbstractInputComponent<>(getSequentialAwareFirstInputPort()) {

            @Override
            protected void tick() {
                sequentialAwareTick();
            }

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }

    static <T> void methodToInputComponent(BufferChainLink<T> inputBuffer, InputCallable<T> method) {
        MethodInputComponent<T> component = new MethodInputComponent<>(inputBuffer, method);
        tryToBreakParallelChain(inputBuffer, component);
        new InputComponentChainLink<>(inputBuffer.previousComponent, component).breakChain();
    }

}
