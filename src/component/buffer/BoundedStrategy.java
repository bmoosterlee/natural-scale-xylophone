package component.buffer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class BoundedStrategy<T> implements BufferStrategy<T> {
    private String name;
    private final ConcurrentLinkedQueue<T> buffer;
    private final Semaphore emptySpots;
    private final Semaphore filledSpots;

    public BoundedStrategy(int capacity, String name) {
        if (name == null) {
            this.name = "unnamed";
        }
        else {
            this.name = name;
        }
        buffer = new ConcurrentLinkedQueue<>();
        emptySpots = new Semaphore(capacity);
        filledSpots = new Semaphore(capacity);

        filledSpots.drainPermits();
    }

    @Override
    public List<T> flush() throws InterruptedException {
        int length = filledSpots.availablePermits();
        int count = 0;

        List<T> list = new LinkedList<>();
        while (!isEmpty() && count < length) {
            list.add(poll());
            count++;
        }
        return list;
    }

    @Override
    public void offer(T packet) throws InterruptedException {
        if (packet == null) {
            throw new NullPointerException();
        }
        if (isFull()) {
            TrafficAnalyzer.logClog(name);
        }
        emptySpots.acquire();
        buffer.offer(packet);
        filledSpots.release();
    }

    @Override
    public T poll() throws InterruptedException {
        filledSpots.acquire();
        T item = buffer.poll();
        emptySpots.release();
        return item;
    }

    @Override
    public T tryPoll() {
        if(filledSpots.tryAcquire()){
            T item = buffer.poll();
            emptySpots.release();
            return item;
        }
        else{
            return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return filledSpots.availablePermits() == 0;
    }

    @Override
    public boolean isFull() {
        return emptySpots.availablePermits() == 0;
    }

    @Override
    public String getName() {
        return name;
    }

    ConcurrentLinkedQueue<T> getBuffer() {
        return buffer;
    }

    Semaphore getEmptySpots() {
        return emptySpots;
    }

    Semaphore getFilledSpots() {
        return filledSpots;
    }
}