package component.buffer;

public class InputComponentChainLink<K> extends ComponentChainLink<K>{
    private final MethodInputComponent<K> methodInputComponent;

    public InputComponentChainLink(PipeComponentChainLink<?, K> previousComponentChainLink, MethodInputComponent<K> methodInputComponent){
        super(previousComponentChainLink);
        this.methodInputComponent = methodInputComponent;
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
        return new InputComponentChainLink<>(inputBuffer.previousComponent, new MethodInputComponent<>(inputBuffer, method));
    }

}
