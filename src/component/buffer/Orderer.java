package component.buffer;

import java.util.PriorityQueue;

public class Orderer<T> extends AbstractPipeComponent<OrderStampedPacket<T>, T> {
    private final PriorityQueue<OrderStampedPacket<T>> backLog;
    private OrderStampedPacket<T> index;

    public Orderer(BoundedBuffer<OrderStampedPacket<T>> input, BoundedBuffer<T> output) {
        super(input.createInputPort(), output.createOutputPort());
        backLog = new PriorityQueue<>();
    }

    @Override
    protected void tick() {
        try {
            OrderStampedPacket<T> newPacket = input.consume();
            if (index != null) {
                backLog.add(newPacket);
                if(!backLog.isEmpty()) {
                    if (index.successor(backLog.peek())){
                        index = backLog.poll();
                        output.produce(index.unwrap());
                    }
                }
            } else {
                index = newPacket;
                output.produce(index.unwrap());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T> PipeCallable<BoundedBuffer<OrderStampedPacket<T>>, BoundedBuffer<T>> buildPipe(){

        return new PipeCallable<>() {
            @Override
            public BoundedBuffer<T> call(BoundedBuffer<OrderStampedPacket<T>> inputBuffer) {
                SimpleBuffer<T> outputBuffer = new SimpleBuffer<>(1, "orderer");
                new Orderer<T>(inputBuffer, outputBuffer);
                return outputBuffer;
            }

            @Override
            public Boolean isParallelisable() {
                return false;
            }
        };

    }
}
