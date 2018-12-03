package component.orderer;

import component.buffer.PipeCallable;
import component.buffer.Packet;
import component.buffer.SimplePacket;

public class OrderStampedPacket<T> extends SimplePacket<T> implements Comparable<OrderStampedPacket<T>>{
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
    public <V, Y extends Packet<V>> Y transform(PipeCallable<T, V> method) {
        return (Y) new OrderStampedPacket<>(this, method);
    }

    public boolean successor(OrderStampedPacket<T> other) {
        return orderStamper.successor(this, other);
    }

    @Override
    public int compareTo(OrderStampedPacket<T> other) {
        return orderStamper.compare(this, other);
    }

    public boolean hasFirstStamp() {
        return orderStamper.firstStamp(this);
    }
}
