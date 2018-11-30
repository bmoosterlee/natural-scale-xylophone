package component.buffer;

public class InputComponentChainLink<K> extends ComponentChainLink<K, Void> {
    private final InputCallable<K> method;
    private final SimpleBuffer<K> inputBuffer;

    private InputComponentChainLink(InputCallable<K> method, BufferChainLink<K> inputBuffer){
        super(inputBuffer.previousComponent);
        this.method = method;
        this.inputBuffer = inputBuffer.getBuffer();
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
            else {
                InputCallable<K> call;
                if (!isParallelisable()) {
                    call = new InputCallable<>() {
                        @Override
                        public void call(K input) {
                            synchronized (this) {
                                method.call(input);
                            }
                        }
                    };
                } else {
                    call = method;
                }
                previousComponentChainLink.wrap(call, 1);
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
    protected void wrap(InputCallable<Void> nextMethod, int chainLinks) {

    }

    static <T> void methodToInputComponent(BufferChainLink<T> inputBuffer, InputCallable<T> method) {
        new InputComponentChainLink<>(method, inputBuffer).breakChain();
    }

}
