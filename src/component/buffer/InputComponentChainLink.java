package component.buffer;

public class InputComponentChainLink<K, A extends Packet<K>> extends ComponentChainLink<K, Void, A, Packet<Void>> {
    private final InputCallable<K> method;
    private final SimpleBuffer<K, A> inputBuffer;

    private InputComponentChainLink(InputCallable<K> method, BufferChainLink<K, A> inputBuffer){
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
    protected <W, C extends Packet<W>> void wrap(PipeCallable<Void, W> nextMethod, SimpleBuffer<W, C> outputBuffer, int chainLinks) {

    }

    @Override
    protected void wrap(InputCallable<Void> nextMethod, int chainLinks) {

    }

    static <T, A extends Packet<T>> void methodToInputComponent(BufferChainLink<T, A> inputBuffer, InputCallable<T> method) {
        new InputComponentChainLink<>(method, inputBuffer).breakChain();
    }

}
