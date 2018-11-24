package component.buffer;

public class InputComponentChainLink<K> extends ComponentChainLink {
    private final MethodInputComponent<K> methodInputComponent;

    public InputComponentChainLink(ComponentChainLink previousComponentChainLink, MethodInputComponent<K> methodInputComponent){
        super(previousComponentChainLink);
        this.methodInputComponent = methodInputComponent;
    }

    @Override
    protected void parallelisationAwareTick() {
        try{
            if(previousComponentChainLink.isParallelisable()) {
                previousComponentChainLink.parallelisationAwareTick();
            }
        }
        catch(NullPointerException ignored){
        }

        componentTick();
    }

    @Override
    InputPort getParallelisationAwareFirstInputPort() {
        try{
            if(previousComponentChainLink.isParallelisable()) {
                return previousComponentChainLink.getParallelisationAwareFirstInputPort();
            }
        }
        catch(NullPointerException ignored) {
        }

        return methodInputComponent.input;
    }

    @Override
    protected void componentTick() {
        methodInputComponent.tick();
    }

    @Override
    protected OutputPort getOutputPort() {
        return null;
    }

    @Override
    protected InputPort getFirstInputPort() {
        return previousComponentChainLink.getFirstInputPort();
    }

    @Override
    Boolean isParallelisable() {
        return methodInputComponent.isParallelisable();
    }

    @Override
    public AbstractComponent wrap() {
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
        parallelChainCheck(inputBuffer, component);
        return new InputComponentChainLink<>(inputBuffer.previousComponent, component);
    }

}
