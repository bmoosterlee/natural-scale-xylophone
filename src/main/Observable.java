package main;

import java.util.Collection;
import java.util.HashSet;

public class Observable<T> {
    private final Collection<Observer<T>> observers;

    public Observable(){
        observers = new HashSet<>();
    }

    public void add(Observer<T> observer) {
        observers.add(observer);
    }

    public void notify(T event) {
        for(Observer<T> observer : observers) {
            observer.notify(event);
        }
    }
}