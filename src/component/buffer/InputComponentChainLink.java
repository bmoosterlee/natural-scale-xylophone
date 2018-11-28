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

    static <T> void methodToInputComponent(BufferChainLink<T> inputBuffer, InputCallable<T> method) {
        MethodInputComponent<T> component = new MethodInputComponent<>(inputBuffer, method);
        new InputComponentChainLink<>(inputBuffer.previousComponent, component).breakChain();
    }

}
