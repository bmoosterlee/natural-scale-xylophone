package component.buffer;

import java.util.Collections;
import java.util.List;

public class InputPort<T> {

    private final BoundedBuffer<T> buffer;

    protected InputPort(BoundedBuffer<T> buffer){
        this.buffer = buffer;
    }

    public T consume() throws InterruptedException {
        return buffer.poll();
    }

    public T tryConsume() {
        return buffer.tryPoll();
    }

    public List<T> flush() throws InterruptedException {
        return buffer.flush();
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
        return buffer.isEmpty();
    }

    public BoundedBuffer<T> getBuffer() {
        return buffer;
    }
}
