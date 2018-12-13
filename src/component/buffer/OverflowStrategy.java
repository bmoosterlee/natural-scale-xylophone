package component.buffer;

import java.util.concurrent.LinkedBlockingQueue;

public class OverflowStrategy<T> extends BoundedStrategy<T> {

    public OverflowStrategy(String name) {
        super(new LinkedBlockingQueue<>(), Integer.MAX_VALUE, name);
    }

    @Override
    public boolean isFull() {
        return !isEmpty();
    }
}
