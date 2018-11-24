package component.buffer;

public interface PipeCallable<K, V> {

    V call(K input);

}
