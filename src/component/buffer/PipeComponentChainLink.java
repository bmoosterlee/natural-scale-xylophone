package component.buffer;

import java.util.Collection;
import java.util.Collections;

public class PipeComponentChainLink<K, V> extends ComponentChainLink<K, V> {
    private final MethodPipeComponent<K, V> methodPipeComponent;

    private PipeComponentChainLink(ComponentChainLink<?, K> previousComponentChainLink, MethodPipeComponent<K, V> methodPipeComponent){
        super(previousComponentChainLink);
        this.methodPipeComponent = methodPipeComponent;
    }

    private PipeComponentChainLink(MethodPipeComponent<K, V> methodPipeComponent){
        this(null, methodPipeComponent);
    }

    @Override
    protected void wrap() {
        if(!isParallelisable()) {
            if(previousComponentChainLink!=null) {
                if (!previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(methodPipeComponent.method, getOutputPort().getBuffer(), 1);
                } else {
                    new TickRunningStrategy(methodPipeComponent, 1);

                    previousComponentChainLink.wrap();
                }
            }
            else{
                new TickRunningStrategy(methodPipeComponent, 1);
            }
        }
        else{
            if(previousComponentChainLink!=null) {
                if (previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(methodPipeComponent.method, getOutputPort().getBuffer(), 1);
                } else {
                    new TickRunningStrategy(methodPipeComponent);

                    previousComponentChainLink.wrap();
                }
            }
            else{
                new TickRunningStrategy(methodPipeComponent);
            }
        }
    }

    @Override
    protected void wrap(PipeCallable<V, ?> nextMethodChain, BoundedBuffer outputBuffer, int chainLinks) {
        if(!isParallelisable()) {
            PipeCallable<K, Object> sequentialCallChain = input -> {
                synchronized (this) {
                    return nextMethodChain.call(methodPipeComponent.method.call(input));
                }
            };

            if(previousComponentChainLink!=null) {
                if (!previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(sequentialCallChain, outputBuffer, chainLinks + 1);
                } else {
                    new TickRunningStrategy(
                            new AbstractComponent<K, Object>() {

                                private OutputPort outputPort = outputBuffer.createOutputPort();

                                @Override
                                protected Collection<BoundedBuffer<K>> getInputBuffers() {
                                    return Collections.singleton(getInputPort().getBuffer());
                                }

                                @Override
                                protected Collection<BoundedBuffer<Object>> getOutputBuffers() {
                                    return Collections.singleton(outputBuffer);
                                }

                                @Override
                                protected void tick() {
                                    try {
                                        outputPort.produce(
                                                sequentialCallChain.call(
                                                        getInputPort().consume()));
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, chainLinks + 1);

                    previousComponentChainLink.wrap();
                }
            }
            else{
                new TickRunningStrategy(
                        new AbstractComponent<K, Object>() {

                            private OutputPort outputPort = outputBuffer.createOutputPort();

                            @Override
                            protected Collection<BoundedBuffer<K>> getInputBuffers() {
                                return Collections.singleton(getInputPort().getBuffer());
                            }

                            @Override
                            protected Collection<BoundedBuffer<Object>> getOutputBuffers() {
                                return Collections.singleton(outputBuffer);
                            }

                            @Override
                            protected void tick() {
                                try {
                                    outputPort.produce(
                                            sequentialCallChain.call(
                                                    getInputPort().consume()));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, chainLinks + 1);
            }
        }
        else {
            PipeCallable<K, Object> parallelCallChain = input -> nextMethodChain.call(methodPipeComponent.method.call(input));

            if (previousComponentChainLink != null){
                if (previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(parallelCallChain, outputBuffer, chainLinks + 1);
                } else {
                    new TickRunningStrategy(new AbstractComponent<K, Object>() {

                        private OutputPort outputPort = outputBuffer.createOutputPort();

                        @Override
                        protected Collection<BoundedBuffer<K>> getInputBuffers() {
                            return Collections.singleton(getInputPort().getBuffer());
                        }

                        @Override
                        protected Collection<BoundedBuffer<Object>> getOutputBuffers() {
                            return Collections.singleton(outputBuffer);
                        }

                        @Override
                        protected void tick() {
                            try {
                                outputPort.produce(
                                        parallelCallChain.call(
                                                getInputPort().consume()));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    previousComponentChainLink.wrap();
                }
            }
            else{
                new TickRunningStrategy(new AbstractComponent<K, Object>() {

                    private OutputPort outputPort = outputBuffer.createOutputPort();

                    @Override
                    protected Collection<BoundedBuffer<K>> getInputBuffers() {
                        return Collections.singleton(getInputPort().getBuffer());
                    }

                    @Override
                    protected Collection<BoundedBuffer<Object>> getOutputBuffers() {
                        return Collections.singleton(outputBuffer);
                    }

                    @Override
                    protected void tick() {
                        try {
                            outputPort.produce(
                                    parallelCallChain.call(
                                            getInputPort().consume()));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void wrap(InputCallable<V> nextMethodChain, BoundedBuffer outputBuffer, int chainLinks) {
        if(!isParallelisable()) {
            InputCallable<K> sequentialCallChain = input -> {
                synchronized (this) {
                    nextMethodChain.call(methodPipeComponent.method.call(input));
                }
            };

            if (previousComponentChainLink != null){
                if (!previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(sequentialCallChain, outputBuffer, chainLinks + 1);
                } else {
                    new TickRunningStrategy(new AbstractInputComponent<>(getInputPort()) {
                        @Override
                        protected void tick() {
                            try {
                                sequentialCallChain.call(getInputPort().consume());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }, chainLinks + 1);

                    previousComponentChainLink.wrap();
                }
            }
            else{
                new TickRunningStrategy(new AbstractInputComponent<>(getInputPort()) {
                    @Override
                    protected void tick() {
                        try {
                            sequentialCallChain.call(getInputPort().consume());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }, chainLinks + 1);
            }
        }
        else{
            InputCallable<K> parallelCallChain = input -> nextMethodChain.call(methodPipeComponent.method.call(input));

            if(previousComponentChainLink!=null) {
                if (previousComponentChainLink.isParallelisable()) {
                    previousComponentChainLink.wrap(parallelCallChain, outputBuffer, chainLinks + 1);
                } else {
                    new TickRunningStrategy(new AbstractInputComponent<>(getInputPort()) {
                        @Override
                        protected void tick() {
                            try {
                                parallelCallChain.call(getInputPort().consume());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    previousComponentChainLink.wrap();
                }
            }
            else{
                new TickRunningStrategy(new AbstractInputComponent<>(getInputPort()) {
                    @Override
                    protected void tick() {
                        try {
                            parallelCallChain.call(getInputPort().consume());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected InputPort<K> getInputPort() {
        return methodPipeComponent.input;
    }

    @Override
    protected OutputPort getOutputPort() {
        return methodPipeComponent.output;
    }

    @Override
    Boolean isParallelisable() {
        return methodPipeComponent.isParallelisable();
    }

    static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(BufferChainLink<K> inputBuffer, PipeCallable<K, V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        MethodPipeComponent<K, V> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, method);
        return getBufferChainLink(inputBuffer, outputBuffer, component);
    }

    private static <K, V> BufferChainLink<V> getBufferChainLink(BufferChainLink<K> inputBuffer, SimpleBuffer<V> outputBuffer, MethodPipeComponent<K, V> component) {
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<>(inputBuffer.previousComponent, component);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    static <K, V> BufferChainLink<V> methodToComponentWithOutputBuffer(SimpleBuffer<K> inputBuffer, PipeCallable<K, V> method, int capacity, String name) {
        SimpleBuffer<V> outputBuffer = new SimpleBuffer<>(capacity, name);
        MethodPipeComponent<K, V> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, method);
        return getBufferChainLink(outputBuffer, component);
    }

    private static <K, V> BufferChainLink<V> getBufferChainLink(SimpleBuffer<V> outputBuffer, MethodPipeComponent<K, V> component) {
        PipeComponentChainLink<K, V> componentChainLink = new PipeComponentChainLink<>(component);
        BufferChainLink<V> outputChainLink = new BufferChainLink<>(outputBuffer, componentChainLink);
        return outputChainLink;
    }

    static <K> BufferChainLink<K> chainToOverwritableBuffer(BufferChainLink<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        MethodPipeComponent<K, K> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input);
        return getBufferChainLink(inputBuffer, outputBuffer, component);
    }

    public static <K> BufferChainLink<K> chainToOverwritableBuffer(SimpleBuffer<K> inputBuffer, int capacity, String name) {
        SimpleBuffer<K> outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>(capacity, name));
        MethodPipeComponent<K, K> component = new MethodPipeComponent<>(inputBuffer, outputBuffer, input -> input);
        return getBufferChainLink(outputBuffer, component);
    }
}
