package main;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class BoundedBuffer<T> {

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

    void offer(T packet) throws InterruptedException {
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
}
