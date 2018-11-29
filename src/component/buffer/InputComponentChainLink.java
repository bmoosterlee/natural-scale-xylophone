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
                InputCallable<K> sequentialCall = new InputCallable<>() {
                    @Override
                    public void call(K input) {
                        synchronized (this) {
                            method.call(input);
                        }
                    }
                };
                previousComponentChainLink.wrap(sequentialCall, 1);
            } else {
                InputCallable<K> parallelCall = method;
                previousComponentChainLink.wrap(parallelCall, 1);
            }
        }
        else{
            startAutonomousComponent();
        }
    }

    private void startAutonomousComponent() {
        new TickRunningStrategy(new MethodInputComponent<>(inputBuffer, method));
    }

    @Override
    protected <W> void wrap(PipeCallable<Void, W> nextMethod, BoundedBuffer<W> outputBuffer, int chainLinks) {

    }

    @Override
    protected <W> void wrap(InputCallable<Void> nextMethod, int chainLinks) {

    }

    static <T> void methodToInputComponent(BufferChainLink<T> inputBuffer, InputCallable<T> method) {
        new InputComponentChainLink<T>(inputBuffer.previousComponent, method, inputBuffer).breakChain();
    }

}
