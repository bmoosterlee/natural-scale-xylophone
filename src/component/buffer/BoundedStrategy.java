package component.buffer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class BoundedStrategy<T> implements BufferStrategy<T> {
    private final int capacity;
    private String name;
    private final ArrayBlockingQueue<T> buffer;

    public BoundedStrategy(int capacity, String name) {
        this.capacity = capacity;
        if (name == null) {
            this.name = "unnamed";
        }
        else {
            this.name = name;
        }
        buffer = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public List<T> flush() throws InterruptedException {
        List<T> list = new LinkedList<>();
        while (!isEmpty()) {
            list.add(poll());
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
        buffer.put(packet);
    }

    @Override
    public T poll() throws InterruptedException {
        return buffer.take();
    }

    @Override
    public T tryPoll() {
        T item = null;
        try {
            item = buffer.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return item;
    }

    @Override
    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    @Override
    public boolean isFull() {
        return buffer.remainingCapacity() == 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return capacity;
    }

    ArrayBlockingQueue<T> getBuffer() {
        return buffer;
    }

}