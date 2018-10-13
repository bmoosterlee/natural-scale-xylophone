package main;

import java.util.LinkedList;
import java.util.List;

public class InputPort<T> {

    private final BoundedBuffer<T> buffer;

    public InputPort(BoundedBuffer<T> buffer){
        this.buffer = buffer;
    }

    public T consume() throws InterruptedException {
        return buffer.poll();
    }

    public List<T> flush() throws InterruptedException {
        int length = buffer.filledSpots.availablePermits();
        int count = 0;

        List<T> list = new LinkedList<>();
        while(buffer.filledSpots.availablePermits() != 0 && count<length){
            list.add(buffer.poll());
            count++;
        }
        return list;
    }
}
