package component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public interface BufferInterface<T> {
    List<T> flush() throws InterruptedException;

    void offer(T packet) throws InterruptedException;

    T poll() throws InterruptedException;

    boolean isEmpty();

    boolean isFull();

    InputPort<T> createInputPort();

    OutputPort<T> createOutputPort();

    <V> BufferChainLink<V> performMethod(CallableWithArguments<T, V> method);

    void performInputMethod(CallableWithArgument<T> method);

    Collection<BufferInterface<T>> broadcast(int size);

    <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferInterface<V> other);

    <V> SimpleBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BufferChainLink<V> other);

    BufferInterface<T> relayTo(SimpleBuffer<T> outputBuffer);
}
