package component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public class BoundedBuffer<T> implements BufferInterface<T> {

    private final BoundedStrategy<T> boundedStrategy;

    public BoundedBuffer(int capacity, String name){
        this(capacity, new BoundedStrategy<>(capacity, name));
    }

    public BoundedBuffer(int capacity, BoundedStrategy<T> strategy){
        boundedStrategy = strategy;

        try {
            boundedStrategy.getFilledSpots().acquire(capacity);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<T> flush() throws InterruptedException {
        return boundedStrategy.flush();
    }

    @Override
    public void offer(T packet) throws InterruptedException {
        boundedStrategy.offer(packet);
    }

    @Override
    public T poll() throws InterruptedException {
        return boundedStrategy.poll();
    }

    @Override
    public boolean isEmpty() {
        return boundedStrategy.isEmpty();
    }

    @Override
    public boolean isFull() {
        return boundedStrategy.isFull();
    }

    @Override
    public InputPort<T> createInputPort(){
        return new InputPort<>(this);
    }

    @Override
    public OutputPort<T> createOutputPort(){
        return new OutputPort<>(this);
    }

    @Override
    public <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method){
        return TickablePipeComponentChain.methodToComponentWithOutputBuffer(new BufferChainLink<>(this, null), method, 1, "performMethod");
    }

    //todo use chain link
    @Override
    public void performInputMethod(CallableWithArgument<T> method){
        new TickableInputComponentChain<>(new BufferChainLink<>(this, null), method);
    }

    @Override
    public Collection<BufferInterface<T>> broadcast(int size) {
        return Broadcast.broadcast(this, size);
    }

    @Override
    public <V> BoundedBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferInterface<V> other){
        return performMethod(Pairer.build(other)).breakChain();
    }

    @Override
    public <V> BoundedBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other){
        return performMethod(Pairer.build(other.breakChain())).breakChain();
    }

    @Override
    public BufferInterface<T> relayTo(BoundedBuffer<T> outputBuffer) {
        new TickablePipeComponent<>(this, outputBuffer, input -> input);
        return outputBuffer;
    }
}
