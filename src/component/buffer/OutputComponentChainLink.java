package component.buffer;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

public class OutputComponentChainLink<V> extends ComponentChainLink<Void, V, Packet<Void>, SimplePacket<V>> {
    private final OutputCallable<V> method;
    private final BoundedBuffer<V, SimplePacket<V>> outputBuffer;

    private OutputComponentChainLink(OutputCallable<V> method, BoundedBuffer<V, SimplePacket<V>> outputBuffer) {
        super(null);
        this.method = method;
        this.outputBuffer = outputBuffer;
    }

    @Override
    protected void wrap() {
        new TickRunningStrategy(new MethodOutputComponent<>(outputBuffer, method));
    }

    @Override
    protected <W, C extends Packet<W>> void wrap(PipeCallable<V, W> nextMethodChain, BoundedBuffer<W, C> outputBuffer, int chainLinks) {
        if(!isParallelisable()) {
            OutputCallable<W> sequentialCallChain = createSequentialLink(nextMethodChain);
            startChainedSequentialComponent(sequentialCallChain, outputBuffer, chainLinks);
        } else {
            OutputCallable<W> parallelCallChain = createParallelLink(nextMethodChain);
            startChainedParallelComponent(parallelCallChain, outputBuffer);
        }
    }

    private <W> OutputCallable<W> createSequentialLink(PipeCallable<V, W> nextMethodChain) {
        return () -> {
            V output;
            synchronized (this) {
                output = method.call();
            }
            return nextMethodChain.call(output);
        };
    }

    private <W> OutputCallable<W> createParallelLink(PipeCallable<V, W> nextMethodChain) {
        return () -> nextMethodChain.call(method.call());
    }

    @Override
    protected void wrap(InputCallable<V> nextMethodChain, int chainLinks) {
        if(!isParallelisable()) {
            Callable<Void> sequentialCallChain = createSequentialLink(nextMethodChain);
            startShutInSequentialComponent(sequentialCallChain, chainLinks);
        } else {
            Callable<Void> parallelCallChain = createParallelLink(nextMethodChain);
            startShutInParallelComponent(parallelCallChain);
        }
    }

    private Callable<Void> createSequentialLink(InputCallable<V> nextMethodChain) {
        return new Callable<>() {
            @Override
            public Void call() {
                V output;
                synchronized (this) {
                    output = method.call();
                }
                nextMethodChain.call(output);
                return null;
            }
        };
    }

    private Callable<Void> createParallelLink(InputCallable<V> nextMethodChain) {
        return () -> {
            nextMethodChain.call(method.call());
            return null;
        };
    }

    private <W, C extends Packet<W>> void startChainedSequentialComponent(OutputCallable<W> sequentialCallChain, BoundedBuffer<W, C> outputBuffer, int chainLinks) {
        new TickRunningStrategy(createMethodOutputComponent(sequentialCallChain, outputBuffer), chainLinks + 1);
    }

    private <W, C extends Packet<W>> void startChainedParallelComponent(OutputCallable<W> parallelCallChain, BoundedBuffer<W, C> outputBuffer) {
        new TickRunningStrategy(createMethodOutputComponent(parallelCallChain, outputBuffer));
    }

    private <W, C extends Packet<W>> AbstractComponent<V, W, SimplePacket<V>, C> createMethodOutputComponent(OutputCallable<W> callChain, BoundedBuffer<W, C> outputBuffer) {
        return new AbstractComponent<>() {
            @Override
            public Collection<BoundedBuffer<V, SimplePacket<V>>> getInputBuffers() {
                return Collections.singleton(OutputComponentChainLink.this.outputBuffer);
            }

            @Override
            public Collection<BoundedBuffer<W, C>> getOutputBuffers() {
                return Collections.singleton(outputBuffer);
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

    private void startShutInSequentialComponent(Callable<Void> sequentialCallChain, int chainLinks) {
        new TickRunningStrategy(
                createShutInComponent(sequentialCallChain),
                chainLinks + 1);
    }

    private void startShutInParallelComponent(Callable<Void> parallelCallChain) {
        new TickRunningStrategy(createShutInComponent(parallelCallChain));
    }

    private AbstractComponent<V, Void, SimplePacket<V>, Packet<Void>> createShutInComponent(Callable<Void> callChain) {
        return new AbstractComponent<>() {
            @Override
            public Collection<BoundedBuffer<V, SimplePacket<V>>> getInputBuffers() {
                return Collections.singleton(OutputComponentChainLink.this.outputBuffer);
            }

            @Override
            public Collection<BoundedBuffer<Void, Packet<Void>>> getOutputBuffers() {
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

    public static <V> BufferChainLink<V, SimplePacket<V>> buildOutputBuffer(OutputCallable<V> method, int capacity, String name) {
        SimpleBuffer<V, SimplePacket<V>> outputBuffer = new SimpleBuffer<>(capacity, name);
        OutputComponentChainLink<V> componentChainLink = new OutputComponentChainLink<>(method, outputBuffer);
        BufferChainLink<V, SimplePacket<V>> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }
}
