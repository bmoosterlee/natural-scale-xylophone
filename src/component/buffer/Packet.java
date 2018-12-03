package component.buffer;

public interface Packet<T> {
    T unwrap();

    <V, Y extends Packet<V>> Y transform(PipeCallable<T, V> method);
}
