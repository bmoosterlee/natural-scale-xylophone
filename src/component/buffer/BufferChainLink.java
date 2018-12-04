package component.buffer;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public class BufferChainLink<K, A extends Packet<K>> implements BoundedBuffer<K, A> {

    private final SimpleBuffer<K, A> buffer;
    protected final ComponentChainLink<?, K, ?, A> previousComponent;

    protected BufferChainLink(SimpleBuffer<K, A> buffer, ComponentChainLink<?, K, ?, A> previousComponent) {
        this.buffer = buffer;
        this.previousComponent = previousComponent;
    }

    protected SimpleBuffer<K, A> breakChain(){
        previousComponent.breakChain();

        return buffer;
    }

    @Override
    public String getName() {
        return buffer.getName();
    }

    @Override
    public List<A> flush() throws InterruptedException {
        return buffer.flush();
    }

    @Override
    public void offer(A packet) throws InterruptedException {
        buffer.offer(packet);
    }

    @Override
    public A poll() throws InterruptedException {
        return buffer.poll();
    }

    @Override
    public A tryPoll() {
        return buffer.tryPoll();
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
    public InputPort<K, A> createInputPort(){
        return breakChain().createInputPort();
    }

    @Override
    public OutputPort<K, A> createOutputPort(){
        return buffer.createOutputPort();
    }

    protected InputPort<K, A> createMethodInternalInputPort(){
        return buffer.createInputPort();
    }

    @Override
    public <V, B extends Packet<V>> BufferChainLink<V, B> performMethod(PipeCallable<K, V> method){
        return performMethod(method,"bufferChainLink - performMethod");
    }

    @Override
    public <V, B extends Packet<V>> BufferChainLink<V, B> performMethod(PipeCallable<K, V> method, String name) {
        return PipeComponentChainLink.methodToComponentWithOutputBuffer(this, method, 1, name);
    }

    @Override
    public void performMethod(InputCallable<K> method){
        InputComponentChainLink.methodToInputComponent(this, method);
    }

    @Override
    public <Z> Z connectTo(PipeCallable<BoundedBuffer<K, A>, Z> pipe) {
        return pipe.call(this);
    }

    @Override
    public void connectTo(InputCallable<BoundedBuffer<K, A>> pipe) {
        pipe.call(this);
    }

    @Override
    public Collection<SimpleBuffer<K, A>> broadcast(int size) {
        return breakChain().broadcast(size);
    }

    @Override
    public Collection<SimpleBuffer<K, A>> broadcast(int size, String name) {
        return breakChain().broadcast(size, name);
    }

    @Override
    public <V, B extends Packet<V>, Y extends Packet<AbstractMap.SimpleImmutableEntry<K, V>>> SimpleBuffer<AbstractMap.SimpleImmutableEntry<K, V>, Y> pairWith(BufferChainLink<V, B> other){
        return breakChain().pairWith(other);
    }

    @Override
    public <V, B extends Packet<V>, Y extends Packet<AbstractMap.SimpleImmutableEntry<K, V>>> SimpleBuffer<AbstractMap.SimpleImmutableEntry<K, V>, Y> pairWith(BufferChainLink<V, B> other, String name){
        return breakChain().pairWith(other, name);
    }

    @Override
    public <V , B extends Packet<V>, Y extends Packet<AbstractMap.SimpleImmutableEntry<K, V>>> SimpleBuffer<AbstractMap.SimpleImmutableEntry<K, V>, Y> pairWith(BoundedBuffer<V, B> other){
        return breakChain().pairWith(other);
    }

    @Override
    public <V, B extends Packet<V>, Y extends Packet<AbstractMap.SimpleImmutableEntry<K, V>>> SimpleBuffer<AbstractMap.SimpleImmutableEntry<K, V>, Y> pairWith(BoundedBuffer<V, B> other, String name){
        return breakChain().pairWith(other, name);
    }

    @Override
    public SimpleBuffer<?, ?> relayTo(SimpleBuffer<? super K, ?> outputBuffer) {
        return breakChain().relayTo(outputBuffer);
    }

    @Override
    public BufferChainLink<K, A> toOverwritable() {
        return PipeComponentChainLink.chainToOverwritableBuffer(this, 1, "to overwritable - output");
    }

    @Override
    public BufferChainLink<K, A> resize(int size) {
        return PipeComponentChainLink.methodToComponentWithOutputBuffer(this, input -> input, size, "buffer expansion");
    }

    @Override
    public BoundedBuffer<K, SimplePacket<K>> rewrap() {
        return breakChain().rewrap();
    }

    SimpleBuffer<K, A> getBuffer(){
        return buffer;
    }
}
