package main;

import java.util.LinkedList;

public class Observable<T> {

    LinkedList<Observer> observers;

    public Observable(){
        observers = new LinkedList<>();
    }

    public void notifyObservers(T event){
        for(Observer observer : observers){
            observer.notify(event);
        }
    }

    public void addObserver(Observer observer){
        observers.add(observer);
    }

}
