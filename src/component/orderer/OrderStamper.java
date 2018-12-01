package component.orderer;

import component.buffer.PipeCallable;

import java.util.Comparator;

public class OrderStamper {
    private long counter;
    private final Comparator<OrderStampedPacket<Object>> comparator;

    public OrderStamper(){
        counter = 0;
        comparator = Comparator.comparingLong(o -> o.orderStamp);
    }

    public static <T> PipeCallable<T, OrderStampedPacket<T>> build(){
        return new PipeCallable<>(){
            OrderStamper orderStamper = new OrderStamper();

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
