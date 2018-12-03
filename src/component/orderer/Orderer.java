package component.orderer;

import component.buffer.*;

import java.util.PriorityQueue;

public class Orderer<T> extends AbstractPipeComponent<T, T, OrderStampedPacket<T>, OrderStampedPacket<T>> {
    private final PriorityQueue<OrderStampedPacket<T>> backLog;
    private OrderStampedPacket<T> index;

    public Orderer(BoundedBuffer<T, OrderStampedPacket<T>> input, BoundedBuffer<T, OrderStampedPacket<T>> output) {
        super(input.createInputPort(), output.createOutputPort());
        backLog = new PriorityQueue<>();
    }

    @Override
    protected void tick() {
        try {
            OrderStampedPacket<T> newPacket = input.consume();
            if (index != null) {
                if(index.successor(newPacket)){
                    index = newPacket;
                    output.produce(newPacket);
                    while (!backLog.isEmpty() && index.successor(backLog.peek())) {
                        index = backLog.poll();
                        output.produce(index);
                    }
                } else {
                    backLog.add(newPacket);
                }
            } else {
                if(newPacket.hasFirstStamp()) {
                    index = newPacket;
                    output.produce(newPacket);
                } else {
                    backLog.add(newPacket);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Boolean isParallelisable(){
        return false;
    }

    public static <T> PipeCallable<BoundedBuffer<T, OrderStampedPacket<T>>, BoundedBuffer<T, OrderStampedPacket<T>>> buildPipe(){
        return inputBuffer -> {
            SimpleBuffer<T, OrderStampedPacket<T>> outputBuffer = new SimpleBuffer<>(1, "orderer");
            new TickRunningStrategy(new Orderer<>(inputBuffer, outputBuffer));
            return outputBuffer;
        };
    }
}
