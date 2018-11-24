package component.buffer;

public interface PipeCallable<K, V> extends ComponentCallable {

    V call(K input);

}
