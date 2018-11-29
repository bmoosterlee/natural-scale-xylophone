package component.buffer;

public class PipeComponentChainLink<K, V> extends ComponentChainLink<K, V> {
    private final PipeCallable<K, V> method;
    private final SimpleBuffer<K> inputBuffer;
    private final BoundedBuffer<V> outputBuffer;

    private PipeComponentChainLink(ComponentChainLink<?, K> previousComponentChainLink, PipeCallable<K, V> method, SimpleBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer){
        super(previousComponentChainLink);
        this.method = method;
        this.inputBuffer = inputBuffer;
        this.outputBuffer = outputBuffer;
    }

    private PipeComponentChainLink(ComponentChainLink<?, K> previousComponentChainLink, PipeCallable<K, V> method, BufferChainLink<K> inputBuffer, BoundedBuffer<V> outputBuffer){
        this(previousComponentChainLink, method, inputBuffer.getBuffer(), outputBuffer);
    }

    private PipeComponentChainLink(PipeCallable<K, V> method, SimpleBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer){
        this(null, method, inputBuffer, outputBuffer);
    }

    private PipeComponentChainLink(PipeCallable<K, V> method, BufferChainLink<K> inputBuffer, BoundedBuffer<V> outputBuffer){
        this(null, method, inputBuffer, outputBuffer);
    }

    @Override
    protected void wrap() {
        if(previousComponentChainLink!=null) {
            if(isParallelisable()!=previousComponentChainLink.isParallelisable()){
                startAutonomousComponent();
                previousComponentChainLink.wrap();
            } else if(!isParallelisable()) {
                PipeCallable<K, V> sequentialMethod = new PipeCallable<>() {
                    @Override
                    public V call(K input) {
                        synchronized (this) {
                            return method.call(input);
                        }
                    }
                };
                previousComponentChainLink.wrap(sequentialMethod, outputBuffer, 1);
            } else {
                PipeCallable<K, V> parallelMethod = method;
                previousComponentChainLink.wrap(parallelMethod, outputBuffer, 1);
            }
        } else {
            startAutonomousComponent();
        }
    }

    private void startAutonomousComponent() {
        new TickRunningStrategy(new MethodPipeComponent<>(inputBuffer, outputBuffer, method));
    }

    @Override
    protected <W> void wrap(PipeCallable<V, W> nextMethodChain, BoundedBuffer<W> outputBuffer, int chainLinks) {
        int newChainLinkCount = chainLinks + 1;
        if(!isParallelisable()) {
            PipeCallable<K, W> sequentialCallChain = input -> {
                synchronized (this) {
                    return nextMethodChain.call(method.call(input));
                }
            };
            if(previousComponentChainLink!=null) {
                if (!previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(sequentialCallChain, outputBuffer, newChainLinkCount);
                } else {
                    startChainedSequentialComponent(sequentialCallChain, outputBuffer, newChainLinkCount);
                    previousComponentChainLink.wrap();
                }
            } else {
                startChainedSequentialComponent(sequentialCallChain, outputBuffer, newChainLinkCount);
            }
        } else {
            PipeCallable<K, W> parallelCallChain = input -> nextMethodChain.call(method.call(input));
            if (previousComponentChainLink != null){
                if (previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(parallelCallChain, outputBuffer, newChainLinkCount);
                } else {
                    startChainedParallelComponent(parallelCallChain, outputBuffer);
                    previousComponentChainLink.wrap();
                }
            } else {
                startChainedParallelComponent(parallelCallChain, outputBuffer);
            }
        }
    }

    private <W> void startChainedParallelComponent(PipeCallable<K, W> parallelCallChain, BoundedBuffer<W> outputBuffer) {
        new TickRunningStrategy(new MethodPipeComponent<>(inputBuffer, outputBuffer, parallelCallChain));
    }

    private <W> void startChainedSequentialComponent(PipeCallable<K, W> sequentialCallChain, BoundedBuffer<W> outputBuffer, int chainLinks) {
        new TickRunningStrategy(new MethodPipeComponent<>(inputBuffer, outputBuffer, sequentialCallChain), chainLinks);
    }

    @Override
    protected <W> void wrap(InputCallable<V> nextMethodChain, int chainLinks) {
        int newChainLinkCount = chainLinks + 1;
        if(!isParallelisable()) {
            InputCallable<K> sequentialCallChain = input -> {
                synchronized (this) {
                    nextMethodChain.call(method.call(input));
                }
            };
            if (previousComponentChainLink != null){
                if (!previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(sequentialCallChain, newChainLinkCount);
                } else {
                    startChainedSequentialComponent(sequentialCallChain, newChainLinkCount);
                    previousComponentChainLink.wrap();
                }
            } else {
                startChainedSequentialComponent(sequentialCallChain, newChainLinkCount);
            }
        } else {
            InputCallable<K> parallelCallChain = input -> nextMethodChain.call(method.call(input));
            if(previousComponentChainLink!=null) {
                if (previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(parallelCallChain, newChainLinkCount);
                } else {
                    startChainedParallelComponent(parallelCallChain);
                    previousComponentChainLink.wrap();
                }
            } else{
                startChainedParallelComponent(parallelCallChain);
            }
        }
    }

    private void startChainedParallelComponent(InputCallable<K> parallelCallChain) {
        new TickRunningStrategy(new MethodInputComponent<>(inputBuffer, parallelCallChain));
    }

    private void startChainedSequentialComponent(InputCallable<K> sequentialCallChain, int newChainLinkCount) {
        new TickRunningStrategy(new MethodInputComponent<>(inputBuffer, sequentialCallChain), newChainLinkCount);
    }

    @Override
    Boolean isParallelisable() {
        return method.isParallelisable();
    }

    static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, PipeCallable<K, V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        return getBufferChainLink(inputBuffer, outputBuffer, method);
    }

    private static <K, V> BufferChainLink<V> getBufferChainLink(BufferChainLink<K> inputBuffer, SimpleBuffer<V> outputBuffer, PipeCallable<K, V> method) {
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<>(inputBuffer.previousComponent, method, inputBuffer, outputBuffer);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(SimpleBuffer<K> inputBuffer, PipeCallable<K, V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        return getBufferChainLink(inputBuffer, outputBuffer, method);
    }

    private static <K, V> BufferChainLink<V> getBufferChainLink(SimpleBuffer<K> inputBuffer, SimpleBuffer<V> outputBuffer, PipeCallable<K, V> method) {
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<>(method, inputBuffer, outputBuffer);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    static <K> BufferChainLink<K> chainToOverwritableBuffer(BufferChainLink<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        PipeCallable<K, K> method = input -> input;
        return getBufferChainLink(inputBuffer, outputBuffer, method);
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(SimpleBuffer<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        PipeCallable<K, K> method = input -> input;
        return getBufferChainLink(inputBuffer, outputBuffer, method);
    }
}
