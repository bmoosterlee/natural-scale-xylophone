package component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class BoundedBuffer<T> {

    private final BoundedStrategy<T> boundedStrategy;

    public BoundedBuffer(int capacity, String name){
        this(capacity, new BoundedStrategy<>(capacity, name));
    }

    public BoundedBuffer(int capacity, BoundedStrategy<T> strategy){
        boundedStrategy = strategy;

        try {
            boundedStrategy.getFilledSpots().acquire(capacity);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<T> flush() throws InterruptedException {
        return boundedStrategy.flush();
    }

    void offer(T packet) throws InterruptedException {
        boundedStrategy.offer(packet);
    }

    T poll() throws InterruptedException {
        return boundedStrategy.poll();
    }

    public boolean isEmpty() {
        return boundedStrategy.isEmpty();
    }

    public boolean isFull() {
        return boundedStrategy.isFull();
    }

    public InputPort<T> createInputPort(){
        return new InputPort<>(this);
    }

    public OutputPort<T> createOutputPort(){
        return new OutputPort<>(this);
    }

    public <V> BoundedBuffer<V> performMethod(CallableWithArguments<T, V> method){
        return TickablePipeComponent.methodToComponentWithOutputBuffer(this, method, 1, "performMethod");
    }

    public void performInputMethod(CallableWithArgument<T> method){
        new TickableInputComponent<>(this, method);
    }

    public Collection<BoundedBuffer<T>> broadcast(int size) {
        return Broadcast.broadcast(this, size);
    }

    public <V> BoundedBuffer<AbstractMap.SimpleImmutableEntry<T, V>> pairWith(BoundedBuffer<V> other){
        return performMethod(Pairer.build(other));
    }

    public BoundedBuffer<T> relayTo(BoundedBuffer<T> outputBuffer) {
        new TickablePipeComponent<>(this, outputBuffer, input -> input);
        return outputBuffer;
    }
}
