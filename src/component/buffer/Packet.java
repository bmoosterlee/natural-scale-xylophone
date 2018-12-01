package component.buffer;

public class Packet<T> {
    private final T content;

    public Packet(T content) {
        this.content = content;
    }

    public <K> Packet(Packet<K> previousPacket, PipeCallable<K, T> method) {
        content = method.call(previousPacket.content);
    }

    public T unwrap() {
        return content;
    }

    public <V> Packet<V> transform(PipeCallable<T, V> method){
        return new Packet<>(this, method);
    }
}
