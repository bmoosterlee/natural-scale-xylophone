package component.buffer;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

public class OutputComponentChainLink<V> extends ComponentChainLink<Void, V> {
    private final OutputCallable<V> method;
    private final BoundedBuffer<V> outputBuffer;

    private OutputComponentChainLink(OutputCallable<V> method, BoundedBuffer<V> outputBuffer) {
        super(null);
        this.method = method;
        this.outputBuffer = outputBuffer;
    }

    @Override
    protected void wrap() {
        new TickRunningStrategy(new MethodOutputComponent<>(outputBuffer, method));
    }

    @Override
    protected <W> void wrap(PipeCallable<V, W> nextMethodChain, BoundedBuffer<W> outputBuffer, int chainLinks) {
        if(!isParallelisable()) {
            OutputCallable<W> sequentialCallChain = () -> {
                synchronized (this) {
                    return nextMethodChain.call(method.call());
                }
            };
            startChainedSequentialComponent(sequentialCallChain, outputBuffer, chainLinks);
        } else {
            OutputCallable<W> parallelCallChain = () -> nextMethodChain.call(method.call());
            startChainedParallelComponent(parallelCallChain, outputBuffer);
        }
    }

    private <W> void startChainedSequentialComponent(OutputCallable<W> sequentialCallChain, BoundedBuffer<W> outputBuffer, int chainLinks) {
        new TickRunningStrategy(new MethodOutputComponent<>(outputBuffer, sequentialCallChain), chainLinks + 1);
    }

    private <W> void startChainedParallelComponent(OutputCallable<W> parallelCallChain, BoundedBuffer<W> outputBuffer) {
        new TickRunningStrategy(new MethodOutputComponent<>(outputBuffer, parallelCallChain));
    }

    @Override
    protected <W> void wrap(InputCallable<V> nextMethodChain, int chainLinks) {
        if(!isParallelisable()) {
            Callable<Void> sequentialCallChain = new Callable<>() {
                @Override
                public Void call() {
                    synchronized (this) {
                        nextMethodChain.call(method.call());
                    }
                    return null;
                }
            };
            startShutInSequentialComponent(sequentialCallChain, chainLinks);
        } else {
            Callable<Void> parallelCallChain = () -> {
                nextMethodChain.call(method.call());
                return null;
            };
            startShutInParallelComponent(parallelCallChain);
        }
    }

    private void startShutInSequentialComponent(Callable<Void> sequentialCallChain, int chainLinks) {
        new TickRunningStrategy(
                createShutInComponent(sequentialCallChain),
                chainLinks + 1);
    }

    private void startShutInParallelComponent(Callable<Void> parallelCallChain) {
        new TickRunningStrategy(createShutInComponent(parallelCallChain));
    }

    private AbstractComponent<V, Void> createShutInComponent(Callable<Void> callChain) {
        return new AbstractComponent<>() {
            @Override
            public Collection<BoundedBuffer<V>> getInputBuffers() {
                return Collections.singleton(OutputComponentChainLink.this.outputBuffer);
            }

            @Override
            public Collection<BoundedBuffer<Void>> getOutputBuffers() {
                return Collections.emptyList();
            }

            @Override
            protected void tick() {
                try {
                    callChain.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    Boolean isParallelisable() {
        return method.isParallelisable();
    }

    public static <V> BufferChainLink<V> buildOutputBuffer(OutputCallable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        OutputComponentChainLink<V> componentChainLink = new OutputComponentChainLink<>(method, outputBuffer);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }
}
