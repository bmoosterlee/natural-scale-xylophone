package component.buffer;

public class InputComponentChainLink<K> {
    private final MethodInputComponent<K> methodInputComponent;
    private final PipeComponentChainLink previousComponentChainLink;

    public InputComponentChainLink(PipeComponentChainLink<?, K> previousComponentChainLink, MethodInputComponent<K> methodInputComponent){
        this.methodInputComponent = methodInputComponent;
        this.previousComponentChainLink = previousComponentChainLink;
    }

    public void tick() {
        try{
            previousComponentChainLink.tick();
        }
        catch(NullPointerException ignored){
        }
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
        try{
            return previousComponentChainLink.getFirstInputPort();
        }
        catch(NullPointerException e) {
            return methodInputComponent.input;
        }
    }

    static <T> InputComponentChainLink<T> methodToInputComponent(BufferChainLink<T> inputBuffer, CallableWithArgument<T> method) {
        return new InputComponentChainLink<>(inputBuffer.previousComponent, new MethodInputComponent<>(inputBuffer, method));
    }

}
