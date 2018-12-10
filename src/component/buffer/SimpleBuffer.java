package component.buffer;

import component.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.List;

public class SimpleBuffer<K, A extends Packet<K>> implements BoundedBuffer<K, A> {

    private final BoundedStrategy<A> boundedStrategy;

    public SimpleBuffer(int capacity, String name){
        this(new BoundedStrategy<>(capacity, name));
    }

    public SimpleBuffer(BoundedStrategy<A> strategy){
        boundedStrategy = strategy;
    }

    @Override
    public String getName() {
        return boundedStrategy.getName();
    }

    @Override
    public int getSize() {
        return boundedStrategy.getSize();
    }

    @Override
    public List<A> flush() throws InterruptedException {
        return boundedStrategy.flush();
    }

    @Override
    public void offer(A packet) throws InterruptedException {
        boundedStrategy.offer(packet);
    }

    @Override
    public A poll() throws InterruptedException {
        return boundedStrategy.poll();
    }

    @Override
    public A tryPoll() {
        return boundedStrategy.tryPoll();
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
    public InputPort<K, A> createInputPort(){
        return new InputPort<>(this);
    }

    @Override
    public OutputPort<K, A> createOutputPort(){
        return new OutputPort<>(this);
    }

    @Override
    public <V, B extends Packet<V>> BufferChainLink<V, B> performMethod(PipeCallable<K, V> method){
        return performMethod(method, "simpleBuffer - performMethod");
    }

    @Override
    public <V, B extends Packet<V>> BufferChainLink<V, B> performMethod(PipeCallable<K, V> method, String name) {
        return performMethod(method, 1, name);
    }

    @Override
    public <V, B extends Packet<V>> BufferChainLink<V, B> performMethod(PipeCallable<K, V> method, int capacity, String name) {
        return PipeComponentChainLink.methodToComponentWithOutputBuffer(this, method, capacity, name);
    }

    public <V, B extends Packet<V>> SimpleBuffer<V, B> performMethodUnchained(PipeCallable<K, V> method, String name) {
        SimpleBuffer<V, B> outputBuffer = new SimpleBuffer<>(1, name);
        new TickRunningStrategy(new MethodPipeComponent<>(this, outputBuffer, method));
        return outputBuffer;
    }

    @Override
    public void performMethod(InputCallable<K> method){
        new TickRunningStrategy(new MethodInputComponent<>(this, method));
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
    public Collection<SimpleBuffer<K, A>> broadcast(int consumers) {
        return Broadcast.broadcast(this, consumers);
    }

    @Override
    public Collection<SimpleBuffer<K, A>> broadcast(int consumers, String name) {
        return Broadcast.broadcast(this, consumers, name);
    }

    @Override
    public <V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pairWith(BoundedBuffer<V, B> other){
        return Pairer.pair(this, other);
    }

    @Override
    public <V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pairWith(BoundedBuffer<V, B> other, String name){
        return Pairer.pair(this, other, name);
    }

    @Override
    public <V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pairWith(BufferChainLink<V, B> other){
        return Pairer.pair(this, other.breakChain());
    }

    @Override
    public <V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pairWith(BufferChainLink<V, B> other, String name){
        return Pairer.pair(this, other.breakChain(), name);
    }

    @Override
    public SimpleBuffer<?, ?> relayTo(SimpleBuffer<? super K, ?> outputBuffer) {
        new TickRunningStrategy(new MethodPipeComponent<>(this, outputBuffer, input -> input));
        return outputBuffer;
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
        SimpleBuffer<K, SimplePacket<K>> rewrap = new SimpleBuffer<>(1, "rewrap");

        new TickRunningStrategy(new AbstractPipeComponent<>(this.createInputPort(), rewrap.createOutputPort()) {
            @Override
            protected void tick() {
                try {
                    output.produce(new SimplePacket<>(input.consume().unwrap()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        return rewrap;
    }
}
