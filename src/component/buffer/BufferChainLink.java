package component.buffer;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public class BufferChainLink<T> implements BoundedBuffer<T> {

    private final SimpleBuffer<T> buffer;
    protected final ChainedPipeComponent<? extends Object, T> previousComponent;

    protected BufferChainLink(SimpleBuffer<T> buffer, ChainedPipeComponent<? extends Object, T> previousComponent) {
        this.buffer = buffer;
        this.previousComponent = previousComponent;
    }

    protected SimpleBuffer<T> breakChain(){
        previousComponent.start();

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

    @Override
    public InputPort<T> createInputPort(){
        return breakChain().createInputPort();
    }

    @Override
    public OutputPort<T> createOutputPort(){
        return buffer.createOutputPort();
    }

    protected InputPort<T> createMethodInternalInputPort(){
        return buffer.createInputPort();
    }

    @Override
    public <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method){
        return performMethod(method,"bufferChainLink - performMethod");
    }

    @Override
    public <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method, String name) {
        return ChainedPipeComponent.methodToComponentWithOutputBuffer(this, method, 1, name);
    }

    @Override
    public void performInputMethod(CallableWithArgument<T> method){
        new ChainedInputComponent<>(this, method);
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
        return breakChain().broadcast(size);
    }

    @Override
    public Collection<SimpleBuffer<T>> broadcast(int size, String name) {
        return breakChain().broadcast(size, name);
    }

    @Override
    public <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other){
        return breakChain().pairWith(other);
    }

    @Override
    public <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BoundedBuffer<V> other){
        return breakChain().pairWith(other);
    }

    @Override
    public SimpleBuffer<T> relayTo(SimpleBuffer<T> outputBuffer) {
        return breakChain().relayTo(outputBuffer);
    }
}
