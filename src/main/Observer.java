package main;

public interface Observer<T> {

    void notify(T event);

}
