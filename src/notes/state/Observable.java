package notes.state;

import java.util.Collection;
import java.util.HashSet;

public class Observable<T> {
    final Collection<Observer<T>> observers;

    public Observable(){
        observers = new HashSet<>();
    }

    public void add(Observer<T> observer) {
        observers.add(observer);
    }

    public void notify(T event) {
        for(Observer observer : observers) {
            observer.notify(event);
        }
    }
}