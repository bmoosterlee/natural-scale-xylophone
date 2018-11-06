package main;

public interface CallableWithArguments<K, V> {

    V call(K input);

}
