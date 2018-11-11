package component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public class BufferChainLink<T> implements BufferInterface<T>{

    private final BoundedBuffer<T> buffer;
    final TickablePipeComponentChain<? extends Object, T> previousComponent;

    public BufferChainLink(BoundedBuffer<T> buffer, TickablePipeComponentChain<? extends Object, T> previousComponent) {
        this.buffer = buffer;
        this.previousComponent = previousComponent;
    }

    public BoundedBuffer<T> breakChain(){
        try {
            new Thread(previousComponent::tick).start();
        }
        catch(NullPointerException ignored){
        }
        return buffer;
    }

    @Override
    public List<T> flush() throws InterruptedException {
        return buffer.flush();
    }

    @Override
    public void offer(T packet) throws InterruptedException {
        buffer.offer(packet);
    }

    @Override
    public T poll() throws InterruptedException {
        return buffer.poll();
    }

    @Override
    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    @Override
    public boolean isFull() {
        return buffer.isFull();
    }

    public InputPort<T> createInputPort(){
        return new InputPort<>(breakChain());
    }

    public OutputPort<T> createOutputPort(){
        return new OutputPort<>(breakChain());
    }

    public <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method){
        return TickablePipeComponentChain.methodToComponentWithOutputBuffer(this, method, 1, "performMethod");
    }

    public void performInputMethod(CallableWithArgument<T> method){
        new TickableInputComponentChain<>(this, method);
    }

    public Collection<BufferInterface<T>> broadcast(int size) {
        return breakChain().broadcast(size);
    }

    public <V> BoundedBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other){
        return pairWith(other.breakChain());
    }

    @Override
    public BufferInterface<T> relayTo(BoundedBuffer<T> outputBuffer) {
        return null;
    }

    public <V> BoundedBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferInterface<V> other){
        return breakChain().pairWith(other);
    }

    public void relayTo(BufferInterface<T> outputBuffer) {
        new TickablePipeComponentChain<>(this, outputBuffer, input -> input);
    }
}
