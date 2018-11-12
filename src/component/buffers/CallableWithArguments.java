package component.buffers;

public interface CallableWithArguments<K, V> {

    V call(K input);

}
