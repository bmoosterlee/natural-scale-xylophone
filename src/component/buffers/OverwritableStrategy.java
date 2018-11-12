package component.buffers;

public class OverwritableStrategy<T> extends BoundedStrategy<T> {
    public OverwritableStrategy(int capacity, String name) {
        super(capacity, name);
    }

    @Override
    public void offer(T packet) throws InterruptedException {
        if(getEmptySpots().availablePermits()==0){
            getBuffer().offer(packet);
            getBuffer().poll();
        }
        else{
            getEmptySpots().acquire();
            getBuffer().offer(packet);
            getFilledSpots().release();
        }
    }
}
