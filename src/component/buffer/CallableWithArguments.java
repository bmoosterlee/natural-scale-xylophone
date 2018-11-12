package component.buffer;

public interface CallableWithArguments<K, V> {

    V call(K input);

}
