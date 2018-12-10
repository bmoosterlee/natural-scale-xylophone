package component.buffer;

import component.Broadcast;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.List;

public interface BoundedBuffer<K, A extends Packet<K>> {
    String getName();

    int getSize();

    List<A> flush() throws InterruptedException;

    void offer(A packet) throws InterruptedException;

    A poll() throws InterruptedException;

    A tryPoll();

    boolean isEmpty();

    boolean isFull();

    InputPort<K, A> createInputPort();

    OutputPort<K, A> createOutputPort();

    <V, B extends Packet<V>> BufferChainLink<V, B> performMethod(PipeCallable<K, V> method);

    <V, B extends Packet<V>> BufferChainLink<V, B> performMethod(PipeCallable<K, V> method, String name);

    <V, B extends Packet<V>> BufferChainLink<V, B> performMethod(PipeCallable<K, V> method, int capacity, String name);

    void performMethod(InputCallable<K> method);

    <Z> Z connectTo(PipeCallable<BoundedBuffer<K, A>, Z> pipe);

    void connectTo(InputCallable<BoundedBuffer<K, A>> pipe);

    Collection<SimpleBuffer<K, A>> broadcast(int consumers);

    Collection<SimpleBuffer<K, A>> broadcast(int consumers, String name);

    Collection<SimpleBuffer<K, A>> broadcast(int consumers, int capacity, String name);

    <V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pairWith(BoundedBuffer<V, B> other);

    <V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pairWith(BoundedBuffer<V, B> other, String name);

    <V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pairWith(BufferChainLink<V, B> other);

    <V, B extends Packet<V>, Y extends Packet<SimpleImmutableEntry<K, V>>> SimpleBuffer<SimpleImmutableEntry<K, V>, Y> pairWith(BufferChainLink<V, B> other, String name);

    SimpleBuffer<?, ?> relayTo(SimpleBuffer<? super K, ?> outputBuffer);

    BufferChainLink<K, A> toOverwritable();

    BufferChainLink<K, A> resize(int size);

    BoundedBuffer<K, SimplePacket<K>> rewrap();
}
