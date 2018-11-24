package component.buffer;

public class InputComponentChainLink<K> extends ComponentChainLink<K>{
    private final MethodInputComponent<K> methodInputComponent;

    public InputComponentChainLink(PipeComponentChainLink<?, K> previousComponentChainLink, MethodInputComponent<K> methodInputComponent){
        super(previousComponentChainLink);
        this.methodInputComponent = methodInputComponent;
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
        methodInputComponent.tick();
    }

    @Override
    protected OutputPort getOutputPort() {
        return null;
    }

    public AbstractInputComponent wrap() {
        return new AbstractInputComponent(getFirstInputPort()) {

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

    @Override
    protected InputPort getFirstInputPort() {
        return previousComponentChainLink.getFirstInputPort();
    }

    @Override
    Boolean isParallelisable() {
        return methodInputComponent.isParallelisable();
    }

    static <T> InputComponentChainLink<T> methodToInputComponent(BufferChainLink<T> inputBuffer, CallableWithArgument<T> method) {
        MethodInputComponent<T> component = new MethodInputComponent<>(inputBuffer, method);
        parallelChainCheck(inputBuffer, component);
        return new InputComponentChainLink<>(inputBuffer.previousComponent, component);
    }

}
