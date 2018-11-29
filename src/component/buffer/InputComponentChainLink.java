package component.buffer;

public class InputComponentChainLink<K> extends ComponentChainLink<K, Void> {
    private final MethodInputComponent<K> methodInputComponent;

    public InputComponentChainLink(ComponentChainLink<?, K> previousComponentChainLink, MethodInputComponent<K> methodInputComponent){
        super(previousComponentChainLink);
        this.methodInputComponent = methodInputComponent;
        this.method = method;
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
    protected void wrap() {
        if(!isParallelisable()) {
            if(previousComponentChainLink!=null) {
                if (!previousComponentChainLink.isParallelisable()) {
                    InputCallable<K> sequentialMethod = new InputCallable<>() {
                        @Override
                        public void call(K input) {
                            synchronized (this) {
                                methodInputComponent.method.call(input);
                            }
                        }
                    };
                    previousComponentChainLink.wrap(sequentialMethod, getInputPort().getBuffer(), 1);
                } else {
                    new TickRunningStrategy(methodInputComponent, 1);

                    previousComponentChainLink.wrap();
                }
            }
            else{
                new TickRunningStrategy(methodInputComponent, 1);
            }
        }
        else{
            if(previousComponentChainLink!=null) {
                if (previousComponentChainLink.isParallelisable()) {
                    InputCallable<K> parallelMethod = methodInputComponent.method;
                    previousComponentChainLink.wrap(parallelMethod, getInputPort().getBuffer(), 1);
                } else {
                    new TickRunningStrategy(methodInputComponent);

                    previousComponentChainLink.wrap();
                }
            }
            else{
                new TickRunningStrategy(methodInputComponent);
            }
        }
    }

    @Override
    protected void wrap(PipeCallable<Void, ?> nextMethod, BoundedBuffer outputBuffer, int chainLinks) {

    }

    @Override
    protected void wrap(InputCallable<Void> nextMethod, BoundedBuffer outputBuffer, int chainLinks) {

    }

    static <T> void methodToInputComponent(BufferChainLink<T> inputBuffer, InputCallable<T> method) {
        MethodInputComponent<T> component = new MethodInputComponent<>(inputBuffer, method);
        new InputComponentChainLink<>(inputBuffer.previousComponent, component).breakChain();
    }

}
