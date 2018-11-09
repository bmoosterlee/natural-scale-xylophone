package component;

import java.util.Collection;
import java.util.LinkedList;
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
}
