package main;

class OverwritableBuffer<T> extends BoundedBuffer<T>{

    public OverwritableBuffer(int capacity) {
        super(capacity);
    }

    public OverwritableBuffer(int capacity, String name) {
        super(capacity, name);
    }

    @Override
    void offer(T packet) throws InterruptedException {
        if(emptySpots.availablePermits()==0){
            buffer.offer(packet);
            buffer.poll();
        }
        else{
            emptySpots.acquire();
            buffer.offer(packet);
            filledSpots.release();
        }
    }

}
