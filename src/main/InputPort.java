package main;

import java.util.Collections;
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
        while(!isEmpty() && count<length){
            list.add(buffer.poll());
            count++;
        }
        return list;
    }

    public List<T> flushOrConsume() throws InterruptedException {
        if(isEmpty()){
            return Collections.singletonList(consume());
        }
        else{
            return flush();
        }
    }

    public boolean isEmpty() {
        return buffer.filledSpots.availablePermits()==0;
    }
}
