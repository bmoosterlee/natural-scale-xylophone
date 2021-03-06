package component.buffer;

import java.util.List;

public interface BufferStrategy<T> {
    List<T> flush() throws InterruptedException;

    void offer(T packet) throws InterruptedException;

    T poll() throws InterruptedException;

    T tryPoll();

    boolean isEmpty();

    boolean isFull();

    String getName();

    int getSize();
}
