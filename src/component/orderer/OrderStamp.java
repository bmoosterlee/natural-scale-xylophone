package component.orderer;

public class OrderStamp {
    private final long index;

    OrderStamp(long index) {
        this.index = index;
    }

    int compareTo(OrderStamp other) {
        return Long.compare(index, other.index);
    }

    OrderStamp successor() {
        return new OrderStamp(index+1);
    }

    boolean successor(OrderStamp other) {
        return (index+1 == other.index);
    }
}
