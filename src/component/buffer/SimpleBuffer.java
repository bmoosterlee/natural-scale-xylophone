package component.buffer;

import component.*;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public class SimpleBuffer<T> implements BoundedBuffer<T> {

    private final BoundedStrategy<T> boundedStrategy;

    public SimpleBuffer(int capacity, String name){
        this(new BoundedStrategy<>(capacity, name));
    }

    public SimpleBuffer(BoundedStrategy<T> strategy){
        boundedStrategy = strategy;
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
        return performMethod(method, "simpleBuffer - performMethod");
    }

    @Override
    public <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method, String name) {
        return PipeComponentChainLink.methodToComponentWithOutputBuffer(this, method, 1, name);
    }

    @Override
    public void performInputMethod(CallableWithArgument<T> method){
        new TickRunningStrategy<>(new MethodInputComponent<>(this, method));
    }

    @Override
    public <V> BoundedBuffer<V> connectTo(CallableWithArguments<BoundedBuffer<T>, BoundedBuffer<V>> pipe) {
        return pipe.call(this);
    }

    @Override
    public void connectTo(CallableWithArgument<BoundedBuffer<T>> pipe) {
        pipe.call(this);
    }

    @Override
    public Collection<SimpleBuffer<T>> broadcast(int size) {
        return Broadcast.broadcast(this, Math.max(10, size));
    }

    @Override
    public Collection<SimpleBuffer<T>> broadcast(int size, String name) {
        return Broadcast.broadcast(this, size, name);
    }

    @Override
    public <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BoundedBuffer<V> other){
        return Pairer.pair(this, other);
    }

    @Override
    public <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other){
        return Pairer.pair(this, other.breakChain());
    }

    @Override
    public SimpleBuffer<T> relayTo(SimpleBuffer<T> outputBuffer) {
        new TickRunningStrategy<>(new MethodPipeComponent<>(this, outputBuffer, input -> input));
        return outputBuffer;
    }

    @Override
    public BufferChainLink<T> toOverwritable() {
        return PipeComponentChainLink.chainToOverwritableBuffer(this, 1, "to overwritable - output");
    }

    @Override
    public BufferChainLink<T> resize(int size) {
        return PipeComponentChainLink.methodToComponentWithOutputBuffer(this, input -> input, size, "buffer expansion");
    }
}
