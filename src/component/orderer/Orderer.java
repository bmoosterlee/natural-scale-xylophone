package component.orderer;

import component.buffer.*;

import java.util.List;
import java.util.PriorityQueue;

public class Orderer<T> extends AbstractPipeComponent<T, T, OrderStampedPacket<T>, OrderStampedPacket<T>> {
    private final PriorityQueue<OrderStampedPacket<T>> backlog;
    private OrderStampedPacket<T> index;

    public Orderer(BoundedBuffer<T, OrderStampedPacket<T>> input, BoundedBuffer<T, OrderStampedPacket<T>> output) {
        super(input.createInputPort(), output.createOutputPort());
        backlog = new PriorityQueue<>();
    }

    @Override
    protected void tick() {
        try {
            List<OrderStampedPacket<T>> newPackets = input.flushOrConsume();

            backlog.addAll(newPackets);
            if (index != null) {
                clearBacklogContinuously();
            } else {
                addPacketsBeforeFindingFirstStamp();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addPacketsBeforeFindingFirstStamp() {
        if(backlog.peek().hasFirstStamp()) {
            OrderStampedPacket<T> firstPacket = backlog.poll();
            index = firstPacket;
            try {
                output.produce(firstPacket);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void clearBacklogContinuously() throws InterruptedException {
        while (!backlog.isEmpty() && index.successor(backlog.peek())) {
            index = backlog.poll();
            output.produce(index);
        }
    }

    @Override
    public Boolean isParallelisable(){
        return false;
    }

    public static <T> PipeCallable<BoundedBuffer<T, OrderStampedPacket<T>>, BoundedBuffer<T, OrderStampedPacket<T>>> buildPipe(String name){
        return inputBuffer -> {
            SimpleBuffer<T, OrderStampedPacket<T>> outputBuffer = new SimpleBuffer<>(1, name);
            new TickRunningStrategy(new Orderer<>(inputBuffer, outputBuffer));
            return outputBuffer;
        };
    }
}
