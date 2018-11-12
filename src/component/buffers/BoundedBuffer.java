package component.buffers;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public interface BoundedBuffer<T> {
    List<T> flush() throws InterruptedException;

    void offer(T packet) throws InterruptedException;

    T poll() throws InterruptedException;

    boolean isEmpty();

    boolean isFull();

    InputPort<T> createInputPort();

    OutputPort<T> createOutputPort();

    <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method);

    void performInputMethod(CallableWithArgument<T> method);

    Collection<BoundedBuffer<T>> broadcast(int size);

    <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BoundedBuffer<V> other);

    <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other);

    BoundedBuffer<T> relayTo(SimpleBuffer<T> outputBuffer);
}
