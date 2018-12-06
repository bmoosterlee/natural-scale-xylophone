package component.orderer;

import component.buffer.PipeCallable;
import component.buffer.Packet;
import component.buffer.SimplePacket;

public class OrderStampedPacket<T> extends SimplePacket<T> implements Comparable<OrderStampedPacket<T>>{
    private final OrderStamper orderStamper;
    public final OrderStamp stamp;

    OrderStampedPacket(OrderStamper orderStamper, OrderStamp stamp, T content) {
        super(content);
        this.orderStamper = orderStamper;
        this.stamp = stamp;
    }

    private <K> OrderStampedPacket(OrderStampedPacket<K> previousPacket, PipeCallable<K, T> method) {
        super(previousPacket, method);
        this.orderStamper = previousPacket.orderStamper;
        this.stamp = previousPacket.stamp;
    }

    @Override
    public <V, Y extends Packet<V>> Y transform(PipeCallable<T, V> method) {
        return (Y) new OrderStampedPacket<>(this, method);
    }

    public boolean successor(OrderStampedPacket<T> other) {
        return stamp.successor(other.stamp);
    }

    @Override
    public int compareTo(OrderStampedPacket<T> other) {
        return stamp.compareTo(other.stamp);
    }

    public boolean hasFirstStamp() {
        return orderStamper.firstStamp(this);
    }
}
