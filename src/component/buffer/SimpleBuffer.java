package component.buffer;

import component.*;
import component.utilities.ChainedPipeComponent;
import component.utilities.RunningInputComponent;
import component.utilities.RunningPipeComponent;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public class SimpleBuffer<T> implements BoundedBuffer<T> {

    private final BoundedStrategy<T> boundedStrategy;

    public SimpleBuffer(int capacity, String name){
        this(capacity, new BoundedStrategy<>(capacity, name));
    }

    public SimpleBuffer(int capacity, BoundedStrategy<T> strategy){
        boundedStrategy = strategy;

        try {
            boundedStrategy.getFilledSpots().acquire(capacity);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return boundedStrategy.getName();
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
        return ChainedPipeComponent.methodToComponentWithOutputBuffer(this, method, 1, "simpleBuffler - performMethod");
    }

    @Override
    public void performInputMethod(CallableWithArgument<T> method){
        new RunningInputComponent<>(this, method);
    }

    @Override
    public Collection<BoundedBuffer<T>> broadcast(int size) {
        return Broadcast.broadcast(this, size);
    }

    @Override
    public <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BoundedBuffer<V> other){
        return performMethod(Pairer.build(other)).breakChain();
    }

    @Override
    public <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other){
        return performMethod(Pairer.build(other.breakChain())).breakChain();
    }

    @Override
    public BoundedBuffer<T> relayTo(SimpleBuffer<T> outputBuffer) {
        new RunningPipeComponent<>(this, outputBuffer, input -> input);
        return outputBuffer;
    }
}
