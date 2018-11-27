package component.buffer;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public interface BoundedBuffer<T> {
    String getName();

    List<T> flush() throws InterruptedException;

    void offer(T packet) throws InterruptedException;

    T poll() throws InterruptedException;

    T tryPoll();

    boolean isEmpty();

    boolean isFull();

    InputPort<T> createInputPort();

    OutputPort<T> createOutputPort();

    <V> BufferChainLink<V> performMethod(PipeCallable<T, V> method);

    <V> BufferChainLink<V> performMethod(PipeCallable<T, V> method, String name);

    void performInputMethod(InputCallable<T> method);

    <V> BoundedBuffer<V> connectTo(PipeCallable<BoundedBuffer<T>, BoundedBuffer<V>> pipe);

    void connectTo(InputCallable<BoundedBuffer<T>> pipe);

    Collection<SimpleBuffer<T>> broadcast(int size);

    Collection<SimpleBuffer<T>> broadcast(int size, String name);

    <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BoundedBuffer<V> other);

    <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other);

    SimpleBuffer<?> relayTo(SimpleBuffer<? super T> outputBuffer);

    BufferChainLink<T> toOverwritable();

    BufferChainLink<T> resize(int size);
}
