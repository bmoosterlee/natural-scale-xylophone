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
    protected void wrap(PipeCallable<V, ?> nextMethodChain, BoundedBuffer outputBuffer, int chainLinks) {
        if(!isParallelisable()) {
            OutputCallable<?> sequentialCallChain = () -> {
                synchronized (this) {
                    return nextMethodChain.call(method.call());
                }
            };
            startChainedSequentialComponent(sequentialCallChain, outputBuffer, chainLinks);
        } else {
            OutputCallable<?> parallelCallChain = () -> nextMethodChain.call(method.call());
            startChainedParallelComponent(parallelCallChain, outputBuffer);
        }
    }

    private void startChainedSequentialComponent(OutputCallable<?> sequentialCallChain, BoundedBuffer outputBuffer, int chainLinks) {
        new TickRunningStrategy(new MethodOutputComponent<>(outputBuffer, sequentialCallChain), chainLinks + 1);
    }

    private void startChainedParallelComponent(OutputCallable<?> parallelCallChain, BoundedBuffer outputBuffer) {
        new TickRunningStrategy(new MethodOutputComponent<>(outputBuffer, parallelCallChain));
    }

    @Override
    protected void wrap(InputCallable<V> nextMethodChain, BoundedBuffer outputBuffer, int chainLinks) {
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

    private AbstractComponent<V, Object> createShutInComponent(Callable<Void> callChain) {
        return new AbstractComponent<>() {
            @Override
            public Collection<BoundedBuffer<V>> getInputBuffers() {
                return Collections.singleton(OutputComponentChainLink.this.outputBuffer);
            }

            @Override
            public Collection<BoundedBuffer<Object>> getOutputBuffers() {
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
