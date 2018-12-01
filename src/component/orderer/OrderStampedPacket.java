package component.orderer;

import component.buffer.Packet;
import component.buffer.PipeCallable;

public class OrderStampedPacket<T> extends Packet<T> implements Comparable<OrderStampedPacket<T>>{
    private final OrderStamper orderStamper;
    final long orderStamp;

    OrderStampedPacket(OrderStamper orderStamper, T content) {
        super(content);
        this.orderStamper = orderStamper;
        orderStamp = orderStamper.stamp();
    }

    private <K> OrderStampedPacket(OrderStampedPacket<K> previousPacket, PipeCallable<K, T> method) {
        super(previousPacket, method);
        this.orderStamper = previousPacket.orderStamper;
        this.orderStamp = previousPacket.orderStamp;
    }

    @Override
    public <V> Packet<V> transform(PipeCallable<T, V> method) {
        return new OrderStampedPacket<V>(this, method);
    }

    public boolean successor(OrderStampedPacket<T> other) {
        return orderStamper.successor(this, other);
    }

    @Override
    public int compareTo(OrderStampedPacket<T> other) {
        return orderStamper.compare(this, other);
    }
}
