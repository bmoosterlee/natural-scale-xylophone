package component.buffer;

import java.util.concurrent.LinkedBlockingQueue;

public class OverwritableStrategy<T> extends BoundedStrategy<T> {

    public OverwritableStrategy(String name) {
        super(new LinkedBlockingQueue<>(), 1, name);
    }

    @Override
    public void offer(T packet) {
        boolean isFull = !isEmpty();
        try {
            super.offer(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(isFull) {
            tryPoll();
        }
    }

    @Override
    public boolean isFull() {
        return false;
    }
}
