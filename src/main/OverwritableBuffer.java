package main;

public class OverwritableBuffer<T> extends BoundedBuffer<T>{

    public OverwritableBuffer(int capacity) {
        super(capacity);
    }

    @Override
    void offer(T packet) throws InterruptedException {
        if(emptySpots.availablePermits()==0){
            buffer.poll();
            buffer.offer(packet);
        }
        else{
            emptySpots.acquire();
            buffer.offer(packet);
            filledSpots.release();
        }
    }
    
}
