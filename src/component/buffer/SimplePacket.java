package component.buffer;

public class SimplePacket<T> implements Packet<T> {
    private final T content;

    public SimplePacket(T content) {
        this.content = content;
    }

    public <K> SimplePacket(Packet<K> previousPacket, PipeCallable<K, T> method) {
        content = method.call(previousPacket.unwrap());
    }

    @Override
    public T unwrap() {
        return content;
    }

    @Override
    public <V, Y extends Packet<V>> Y transform(PipeCallable<T, V> method){
        return (Y) new SimplePacket<>(this, method);
    }
}
