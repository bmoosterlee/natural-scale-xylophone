package main;

public class InputPort<T> {

    private final BoundedBuffer<T> buffer;

    public InputPort(BoundedBuffer<T> buffer){
        this.buffer = buffer;
    }

    public T consume() throws InterruptedException {
        return buffer.poll();
    }

}
