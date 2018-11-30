package component.orderer;

import component.buffer.PipeCallable;

import java.util.Comparator;

public class OrderStamper<T> {
    private long counter;
    private final Comparator<OrderStampedPacket<T>> comparator;

    public OrderStamper(){
        counter = 0;
        comparator = Comparator.comparingLong(o -> o.orderStamp);
    }

    public static <T> PipeCallable<T, OrderStampedPacket<T>> build(){
        return new PipeCallable<>(){
            OrderStamper<T> orderStamper = new OrderStamper<>();

            @Override
            public OrderStampedPacket<T> call(T input) {
                return new OrderStampedPacket<>(orderStamper, input);
            }

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }

    public int compare(OrderStampedPacket<T> packet1, OrderStampedPacket<T> packet2) {
        return comparator.compare(packet1, packet2);
    }

    public long stamp() {
        long orderStamp = counter;
        counter++;
        return orderStamp;
    }

    public boolean successor(OrderStampedPacket<T> first, OrderStampedPacket<T> second) {
        return first.orderStamp+1 == second.orderStamp;
    }
}
