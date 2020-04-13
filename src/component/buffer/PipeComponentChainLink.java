package component.buffer;

public class PipeComponentChainLink<K, V, A extends Packet<K>, B extends Packet<V>> extends ComponentChainLink<K, V, A, B> {
    private final PipeCallable<K, V> method;
    private final SimpleBuffer<K, A> inputBuffer;
    private final SimpleBuffer<V, B> outputBuffer;

    private PipeComponentChainLink(ComponentChainLink<?, K, ?, A> previousComponentChainLink, PipeCallable<K, V> method, SimpleBuffer<K, A> inputBuffer, SimpleBuffer<V, B> outputBuffer){
        super(previousComponentChainLink);
        this.method = method;
        this.inputBuffer = inputBuffer;
        this.outputBuffer = outputBuffer;
    }

    private PipeComponentChainLink(PipeCallable<K, V> method, SimpleBuffer<K, A> inputBuffer, SimpleBuffer<V, B> outputBuffer){
        this(null, method, inputBuffer, outputBuffer);
    }

    private PipeComponentChainLink(PipeCallable<K, V> method, SimpleBuffer<K, A> inputBuffer, BufferChainLink<V, B> outputBuffer){
        this(null, method, inputBuffer, outputBuffer.getBuffer());
    }

    private PipeComponentChainLink(PipeCallable<K, V> method, BufferChainLink<K, A> inputBuffer, SimpleBuffer<V, B> outputBuffer){
        this(inputBuffer.previousComponent, method, inputBuffer.getBuffer(), outputBuffer);
    }

    private PipeComponentChainLink(PipeCallable<K, V> method, BufferChainLink<K, A> inputBuffer, BufferChainLink<V, B> outputBuffer){
        this(inputBuffer.previousComponent, method, inputBuffer.getBuffer(), outputBuffer.getBuffer());
    }

    @Override
    protected void wrap() {
        if(previousComponentChainLink!=null) {
            PipeCallable<K, V> call;
            call = createSequentialCall();
            previousComponentChainLink.wrap(call, outputBuffer, 1);
        } else {
            startAutonomousComponent();
        }
    }

    private PipeCallable<K, V> createSequentialCall() {
        return new PipeCallable<>() {
            @Override
            public V call(K input) {
                synchronized (this) {
                    return method.call(input);
                }
            }
        };
    }

    private PipeCallable<K, V> createParallelCall() {
        return method;
    }

    private void startAutonomousComponent() {
        new TickRunningStrategy(new MethodPipeComponent<>(inputBuffer, outputBuffer, method));
    }

    @Override
    protected <W, C extends Packet<W>> void wrap(PipeCallable<V, W> nextMethodChain, SimpleBuffer<W, C> outputBuffer, int chainLinks) {
        int newChainLinkCount = chainLinks + 1;
        PipeCallable<K, W> sequentialCallChain = createSequentialLink(nextMethodChain);
        if(previousComponentChainLink!=null) {
            previousComponentChainLink.wrap(sequentialCallChain, outputBuffer, newChainLinkCount);
        } else {
            startChainedSequentialComponent(sequentialCallChain, outputBuffer, newChainLinkCount);
        }
    }

    private <W> PipeCallable<K, W> createSequentialLink(PipeCallable<V, W> nextMethodChain) {
        return input -> {
            V output;
            synchronized (this) {
                output = method.call(input);
            }
            return nextMethodChain.call(output);
        };
    }

    private <W> PipeCallable<K, W> createParallelLink(PipeCallable<V, W> nextMethodChain) {
        return input -> nextMethodChain.call(method.call(input));
    }

    private <W, C extends Packet<W>> void startChainedParallelComponent(PipeCallable<K, W> parallelCallChain, SimpleBuffer<W, C> outputBuffer) {
        new TickRunningStrategy(new MethodPipeComponent<>(inputBuffer, outputBuffer, parallelCallChain));
    }

    private <W, C extends Packet<W>> void startChainedSequentialComponent(PipeCallable<K, W> sequentialCallChain, BoundedBuffer<W, C> outputBuffer, int chainLinks) {
        new TickRunningStrategy(new MethodPipeComponent<>(inputBuffer, outputBuffer, sequentialCallChain));
    }

    @Override
    protected void wrap(InputCallable<V> nextMethodChain, int chainLinks) {
        int newChainLinkCount = chainLinks + 1;
        InputCallable<K> sequentialCallChain = createSequentialLink(nextMethodChain);
        if (previousComponentChainLink != null){
            previousComponentChainLink.wrap(sequentialCallChain, newChainLinkCount);
        } else {
            startChainedSequentialComponent(sequentialCallChain);
        }
    }

    private InputCallable<K> createSequentialLink(InputCallable<V> nextMethodChain) {
        return input -> {
            V output;
            synchronized (this) {
                output = method.call(input);
            }
            nextMethodChain.call(output);
        };
    }

    private InputCallable<K> createParallelLink(InputCallable<V> nextMethodChain) {
        return input -> nextMethodChain.call(method.call(input));
    }

    private void startChainedParallelComponent(InputCallable<K> parallelCallChain) {
        new TickRunningStrategy(new MethodInputComponent<>(inputBuffer, parallelCallChain));
    }

    private void startChainedSequentialComponent(InputCallable<K> sequentialCallChain) {
        new TickRunningStrategy(new MethodInputComponent<>(inputBuffer, sequentialCallChain));
    }

    static <K, V, A extends Packet<K>, B extends Packet<V>> BufferChainLink<V, B> methodToComponentWithOutputBuffer(BufferChainLink<K, A> inputBuffer, PipeCallable<K, V> method, int capacity, String name) {
        SimpleBuffer<V, B> outputBuffer = new SimpleBuffer<>(capacity, name);
        return getBufferChainLink(inputBuffer, outputBuffer, method);
    }

    private static <K, V, A extends Packet<K>, B extends Packet<V>> BufferChainLink<V, B> getBufferChainLink(BufferChainLink<K, A> inputBuffer, SimpleBuffer<V, B> outputBuffer, PipeCallable<K, V> method) {
        PipeComponentChainLink<K, V, A, B> componentChainLink = new PipeComponentChainLink<>(method, inputBuffer, outputBuffer);
        BufferChainLink<V, B> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    static <K, V, A extends Packet<K>, B extends Packet<V>> BufferChainLink<V, B> methodToComponentWithOutputBuffer(SimpleBuffer<K, A> inputBuffer, PipeCallable<K, V> method, int capacity, String name) {
        SimpleBuffer<V, B> outputBuffer = new SimpleBuffer<>(capacity, name);
        return getBufferChainLink(inputBuffer, outputBuffer, method);
    }

    private static <K, V, A extends Packet<K>, B extends Packet<V>> BufferChainLink<V, B> getBufferChainLink(SimpleBuffer<K, A> inputBuffer, SimpleBuffer<V, B> outputBuffer, PipeCallable<K, V> method) {
        PipeComponentChainLink<K, V, A, B> componentChainLink = new PipeComponentChainLink<>(method, inputBuffer, outputBuffer);
        BufferChainLink<V, B> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    static <K, A extends Packet<K>> BufferChainLink<K, A> chainToOverwritableBuffer(BufferChainLink<K, A> inputBuffer, String name) {
        SimpleBuffer<K, A> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(name));
        PipeCallable<K, K> method = input -> input;
        return getBufferChainLink(inputBuffer, outputBuffer, method);
    }

    public static <K, A extends Packet<K>> BufferChainLink<K, A> chainToOverwritableBuffer(SimpleBuffer<K, A> inputBuffer, String name) {
        SimpleBuffer<K, A> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(name));
        PipeCallable<K, K> method = input -> input;
        return getBufferChainLink(inputBuffer, outputBuffer, method);
    }
}
