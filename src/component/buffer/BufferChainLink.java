package component.buffer;

import component.utilities.ChainedInputComponent;
import component.utilities.ChainedPipeComponent;
import component.utilities.TickRunner;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public class BufferChainLink<T> implements BoundedBuffer<T> {

    private final SimpleBuffer<T> buffer;
    public final ChainedPipeComponent<? extends Object, T> previousComponent;

    public BufferChainLink(SimpleBuffer<T> buffer, ChainedPipeComponent<? extends Object, T> previousComponent) {
        this.buffer = buffer;
        this.previousComponent = previousComponent;
    }

    public SimpleBuffer<T> breakChain(){
        if(previousComponent!=null) {
            new TickRunner() {

                @Override
                protected void tick() {
                    previousComponent.tick();
                }

            }
            .start();
        }

        return buffer;
    }

    @Override
    public String getName() {
        return buffer.getName();
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
        return breakChain().createInputPort();
    }

    public OutputPort<T> createOutputPort(){
        return createOutputPort();
    }

    public <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method){
        return ChainedPipeComponent.methodToComponentWithOutputBuffer(this, method, 1, "performMethod");
    }

    @Override
    public void performInputMethod(CallableWithArgument<T> method){
        new ChainedInputComponent<>(this, method);
    }

    public Collection<BoundedBuffer<T>> broadcast(int size) {
        return breakChain().broadcast(size);
    }

    public <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other){
        return pairWith(other.breakChain());
    }

    @Override
    public BoundedBuffer<T> relayTo(SimpleBuffer<T> outputBuffer) {
        return null;
    }

    public <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BoundedBuffer<V> other){
        return breakChain().pairWith(other);
    }

    public void relayTo(BoundedBuffer<T> outputBuffer) {
        new ChainedPipeComponent<>(this, outputBuffer, input -> input);
    }
}
