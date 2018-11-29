package component.buffer;

import java.util.Collection;
import java.util.Collections;

public class OutputComponentChainLink<V> extends ComponentChainLink<Void, V> {
    private final MethodOutputComponent<V> methodOutputComponent;

    private OutputComponentChainLink(MethodOutputComponent<V> methodOutputComponent) {
        super(null);
        this.methodOutputComponent = methodOutputComponent;
    }

    @Override
    protected void componentTick() {
        methodOutputComponent.tick();
    }

    @Override
    protected void wrap() {
        new TickRunningStrategy(methodOutputComponent);
    }

    @Override
    protected void wrap(PipeCallable<V, ?> nextMethodChain, BoundedBuffer outputBuffer, int chainLinks) {
        if(!isParallelisable()) {
            new TickRunningStrategy(
                    new MethodOutputComponent<>(outputBuffer, () -> {
                        synchronized (this) {
                            return nextMethodChain.call(methodOutputComponent.method.call());
                        }
                    }) {
                        @Override
                        public Collection<BoundedBuffer> getInputBuffers() {
                            return Collections.singleton(getOutputPort().getBuffer());
                        }
                    }, chainLinks + 1);
        }
        else{
            new TickRunningStrategy(
                    new MethodOutputComponent<>(outputBuffer, () -> nextMethodChain.call(methodOutputComponent.method.call())) {
                        @Override
                        public Collection<BoundedBuffer> getInputBuffers() {
                            return Collections.singleton(getOutputPort().getBuffer());
                        }
                    });
        }
    }

    @Override
    protected void wrap(InputCallable<V> nextMethodChain, BoundedBuffer outputBuffer, int chainLinks) {
        if(!isParallelisable()) {
            new TickRunningStrategy(
                    new AbstractComponent<>() {
                        @Override
                        public Collection<BoundedBuffer<Object>> getInputBuffers() {
                            return Collections.singleton(getOutputPort().getBuffer());
                        }

                        @Override
                        public Collection<BoundedBuffer<Object>> getOutputBuffers() {
                            return Collections.emptyList();
                        }

                        @Override
                        protected void tick() {
                            synchronized (this) {
                                nextMethodChain.call(methodOutputComponent.method.call());
                            }
                        }
                    },
                    chainLinks + 1);
        }
        else{
            new TickRunningStrategy(
                    new AbstractComponent<>() {
                        @Override
                        public Collection<BoundedBuffer<Object>> getInputBuffers() {
                            return Collections.singleton(getOutputPort().getBuffer());
                        }

                        @Override
                        public Collection<BoundedBuffer<Object>> getOutputBuffers() {
                            return Collections.emptyList();
                        }

                        @Override
                        protected void tick() {
                            nextMethodChain.call(methodOutputComponent.method.call());
                        }
                    });
        }
    }

    @Override
    Boolean isParallelisable() {
        return methodOutputComponent.isParallelisable();
    }

    @Override
    protected InputPort getInputPort() {
        return null;
    }

    @Override
    protected OutputPort getOutputPort() {
        return methodOutputComponent.output;
    }

    public static <V> BufferChainLink<V> buildOutputBuffer(OutputCallable<V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        MethodOutputComponent<V> component = new MethodOutputComponent<>(outputBuffer, method);
        OutputComponentChainLink<V> componentChainLink = new OutputComponentChainLink<>(component);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }
}
