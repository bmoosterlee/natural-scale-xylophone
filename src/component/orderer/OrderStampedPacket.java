package component.orderer;

public class OrderStampedPacket<T> implements Comparable<OrderStampedPacket<T>>{
    private final OrderStamper<T> orderStamper;
    public final long orderStamp;
    private final T content;

    public OrderStampedPacket(OrderStamper<T> orderStamper, T content) {
        this.orderStamper = orderStamper;
        this.content = content;
        orderStamp = orderStamper.stamp();
    }

    public T unwrap() {
        return content;
    }

    public boolean successor(OrderStampedPacket<T> other) {
        return orderStamper.successor(this, other);
    }

    @Override
    public int compareTo(OrderStampedPacket<T> other) {
        return orderStamper.compare(this, other);
    }
}
