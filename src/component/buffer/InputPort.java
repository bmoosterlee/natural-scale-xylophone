package component.buffer;

import java.util.Collections;
import java.util.List;

public class InputPort<K, A extends Packet<K>> {

    private final BoundedBuffer<K, A> buffer;

    protected InputPort(BoundedBuffer<K, A> buffer){
        this.buffer = buffer;
    }

    public A consume() throws InterruptedException {
        return buffer.poll();
    }

    public A tryConsume() {
        return buffer.tryPoll();
    }

    public List<A> flush() throws InterruptedException {
        return buffer.flush();
    }

    public List<A> flushOrConsume() throws InterruptedException {
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

    public BoundedBuffer<K, A> getBuffer() {
        return buffer;
    }
}
