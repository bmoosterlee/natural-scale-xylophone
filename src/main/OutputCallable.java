package main;

import component.buffer.ComponentCallable;

public interface OutputCallable<V> extends ComponentCallable {

    V call();

}
