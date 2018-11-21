package component.buffer;

public class InputComponentChainLink<K> extends ComponentChainLink<K>{
    private final MethodInputComponent<K> methodInputComponent;

    public InputComponentChainLink(PipeComponentChainLink<?, K> previousComponentChainLink, MethodInputComponent<K> methodInputComponent){
        super(previousComponentChainLink);
        this.methodInputComponent = methodInputComponent;
    }

    protected void parallisationAwareTick() {
        try{
            if(previousComponentChainLink.methodPipeComponent.isParallelisable() == methodInputComponent.isParallelisable()) {
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

    public AbstractInputComponent wrap() {
        return new AbstractInputComponent(getFirstInputPort()) {

            @Override
            protected void tick() {
                InputComponentChainLink.this.tick();
            }
        };
    }

    private InputPort getFirstInputPort() {
        return previousComponentChainLink.getFirstInputPort();
    }

    static <T> InputComponentChainLink<T> methodToInputComponent(BufferChainLink<T> inputBuffer, CallableWithArgument<T> method) {
        MethodInputComponent<T> component = new MethodInputComponent<>(inputBuffer, method);
        parallelChainCheck(inputBuffer, component);
        return new InputComponentChainLink<>(inputBuffer.previousComponent, component);
    }

}
