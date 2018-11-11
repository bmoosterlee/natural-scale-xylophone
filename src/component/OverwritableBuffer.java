package component;

public class OverwritableBuffer<T> extends BoundedBuffer<T>{

    public OverwritableBuffer(int capacity, String name) {
        super(capacity, new OverwritableStrategy<>(capacity, name));
    }

    private static class OverwritableStrategy<T> extends BoundedStrategy<T> {
        OverwritableStrategy(int capacity, String name) {
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
}
