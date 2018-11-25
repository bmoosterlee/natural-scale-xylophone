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
        return new AbstractInputComponent<>(getFirstInputPort()) {

            @Override
            protected void tick() {
                InputComponentChainLink.this.tick();
            }


            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }

    static <T> InputComponentChainLink<T> methodToInputComponent(BufferChainLink<T> inputBuffer, InputCallable<T> method) {
        MethodInputComponent<T> component = new MethodInputComponent<>(inputBuffer, method);
        tryToBreakParallelChain(inputBuffer, component);
        return new InputComponentChainLink<>(inputBuffer.previousComponent, component);
    }

}
