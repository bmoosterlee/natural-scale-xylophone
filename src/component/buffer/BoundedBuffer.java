package component.buffer;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public interface BoundedBuffer<T> {
    String getName();

    List<T> flush() throws InterruptedException;

    void offer(T packet) throws InterruptedException;

    T poll() throws InterruptedException;

    boolean isEmpty();

    boolean isFull();

    InputPort<T> createInputPort();

    OutputPort<T> createOutputPort();

    <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method);

    <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method, String name);

    void performInputMethod(CallableWithArgument<T> method);

    <V> BoundedBuffer<V> connectTo(CallableWithArguments<BoundedBuffer<T>, BoundedBuffer<V>> pipeline);

    void connectTo(CallableWithArgument<BoundedBuffer<T>> pipeline);

    Collection<SimpleBuffer<T>> broadcast(int size);

    Collection<SimpleBuffer<T>> broadcast(int size, String name);

    <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BoundedBuffer<V> other);

    <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other);

    SimpleBuffer<T> relayTo(SimpleBuffer<T> outputBuffer);
}
