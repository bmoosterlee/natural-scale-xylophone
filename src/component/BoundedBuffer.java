package component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class BoundedBuffer<T> {

    private String name;

    final ConcurrentLinkedQueue<T> buffer;
    final Semaphore emptySpots;
    final Semaphore filledSpots;

    public BoundedBuffer(int capacity){
        buffer = new ConcurrentLinkedQueue<>();

        emptySpots = new Semaphore(capacity);
        filledSpots = new Semaphore(capacity);

        try {
            filledSpots.acquire(capacity);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public BoundedBuffer(int capacity, String name){
        this(capacity);
        this.name = name;
    }

    public List<T> flush() throws InterruptedException {
        int length = filledSpots.availablePermits();
        int count = 0;

        List<T> list = new LinkedList<>();
        while(!isEmpty() && count<length){
            list.add(poll());
            count++;
        }
        return list;
    }

    void offer(T packet) throws InterruptedException {
        if(packet == null){
            throw new NullPointerException();
        }
        if(isFull()) {
            String fixedName = name;
            if(fixedName==null){
                fixedName = "unnamed";
            }
            System.out.println(fixedName + " is clogged up.");
        }
        emptySpots.acquire();
        buffer.offer(packet);
        filledSpots.release();
    }

    T poll() throws InterruptedException {
        filledSpots.acquire();
        T item = buffer.poll();
        emptySpots.release();
        return item;
    }

    public boolean isEmpty() {
        return filledSpots.availablePermits()==0;
    }

    public boolean isFull() {
        return emptySpots.availablePermits()==0;
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

    public <V> BoundedBuffer<V> performOutputMethod(Callable<V> method){
        return TickableOutputComponent.buildOutputBuffer(method, 1, "performOutputMethod");
    }

    public static <K, V> Collection<BoundedBuffer<V>> forEach(Collection<BoundedBuffer<K>> inputBuffers, CallableWithArguments<K, V> method){
        Collection<BoundedBuffer<V>> results = new LinkedList<>();
        for(BoundedBuffer<K> inputBuffer : inputBuffers){
            results.add(inputBuffer.performMethod(method));
        }
        return results;
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
