package component.buffer;

public class InputComponentChainLink<K> extends ComponentChainLink<K, Void> {
    private final InputCallable<K> method;
    private final SimpleBuffer<K> inputBuffer;

    public InputComponentChainLink(ComponentChainLink<?, K> previousComponentChainLink, InputCallable<K> method, SimpleBuffer<K> inputBuffer){
        super(previousComponentChainLink);
        this.method = method;
        this.inputBuffer = inputBuffer;
    }

    public InputComponentChainLink(ComponentChainLink<?, K> previousComponentChainLink, InputCallable<K> method, BufferChainLink<K> inputBuffer){
        this(previousComponentChainLink, method, inputBuffer.getBuffer());
    }


    @Override
    protected InputPort<K> getInputPort() {
        return null;
    }

    @Override
    protected OutputPort getOutputPort() {
        return null;
    }

    @Override
    Boolean isParallelisable() {
        return method.isParallelisable();
    }

    @Override
    protected void wrap() {
        if(previousComponentChainLink!=null) {
            if(isParallelisable()!=previousComponentChainLink.isParallelisable()){
                startAutonomousComponent();
                previousComponentChainLink.wrap();
            }
            else if (!isParallelisable()) {
                InputCallable<K> sequentialMethod = new InputCallable<>() {
                    @Override
                    public void call(K input) {
                        synchronized (this) {
                            method.call(input);
                        }
                    }
                };
                previousComponentChainLink.wrap(sequentialMethod, inputBuffer, 1);
            } else {
                InputCallable<K> parallelMethod = method;
                previousComponentChainLink.wrap(parallelMethod, inputBuffer, 1);
            }
        }
        else{
            startAutonomousComponent();
        }
    }

    private void startAutonomousComponent() {
        InputPort<K> inputPort = inputBuffer.createInputPort();

        new TickRunningStrategy(new MethodInputComponent<>(inputBuffer, method));
    }

    @Override
    protected void wrap(PipeCallable<Void, ?> nextMethod, BoundedBuffer outputBuffer, int chainLinks) {

    }

    @Override
    protected void wrap(InputCallable<Void> nextMethod, BoundedBuffer outputBuffer, int chainLinks) {

    }

    static <T> void methodToInputComponent(BufferChainLink<T> inputBuffer, InputCallable<T> method) {
        new InputComponentChainLink<T>(inputBuffer.previousComponent, method, inputBuffer).breakChain();
    }

}
