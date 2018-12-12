package component.buffer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class BoundedStrategy<T> implements BufferStrategy<T> {
    private final int capacity;
    private String name;
    private final BlockingQueue<T> buffer;

    BoundedStrategy(BlockingQueue<T> queue, int capacity, String name) {
        this.capacity = capacity;
        if (name == null) {
            this.name = "unnamed";
        }
        else {
            this.name = name;
        }
        buffer = queue;
    }

    public BoundedStrategy(int capacity, String name) {
        this(new ArrayBlockingQueue<>(capacity), capacity, name);
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
        return buffer.poll();
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

}