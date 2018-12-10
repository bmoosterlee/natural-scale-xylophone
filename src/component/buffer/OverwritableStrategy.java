package component.buffer;

public class OverwritableStrategy<T> extends BoundedStrategy<T> {
    public OverwritableStrategy(int capacity, String name) {
        super(capacity, name);
    }

    @Override
    public void offer(T packet) {
        if(isFull()){
            getBuffer().offer(packet);
            getBuffer().poll();
        }
        else{
            getBuffer().offer(packet);
        }
    }
}
