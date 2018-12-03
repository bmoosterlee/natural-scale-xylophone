package component.orderer;

import component.buffer.*;

import java.util.Comparator;

public class OrderStamper {
    private long counter;
    private final Comparator<OrderStampedPacket<Object>> comparator;

    public OrderStamper(){
        counter = 0;
        comparator = Comparator.comparingLong(o -> o.orderStamp);
    }

    public static <T, A extends Packet<T>> PipeCallable<BoundedBuffer<T, A>, BoundedBuffer<T, OrderStampedPacket<T>>> buildPipe(){
        return new PipeCallable<>(){
            @Override
            public BoundedBuffer<T, OrderStampedPacket<T>> call(BoundedBuffer<T, A> inputBuffer) {
                SimpleBuffer<T, OrderStampedPacket<T>> outputBuffer = new SimpleBuffer<>(1, "order stamper");

                InputPort<T, A> inputPort = inputBuffer.createInputPort();
                new TickRunningStrategy(new AbstractPipeComponent<>(inputPort, outputBuffer.createOutputPort()) {
                    OrderStamper orderStamper = new OrderStamper();

                    @Override
                    protected void tick() {
                        try {
                            output.produce(
                                    new OrderStampedPacket<>(
                                            orderStamper,
                                            input.consume().unwrap()));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public Boolean isParallelisable(){
                        return false;
                    }
                });

                return outputBuffer;
            }

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }

    public int compare(OrderStampedPacket packet1, OrderStampedPacket packet2) {
        return comparator.compare(packet1, packet2);
    }

    public long stamp() {
        long orderStamp = counter;
        counter++;
        return orderStamp;
    }

    public boolean successor(OrderStampedPacket first, OrderStampedPacket second) {
        return first.orderStamp+1 == second.orderStamp;
    }
}
