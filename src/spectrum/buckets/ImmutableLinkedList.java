package spectrum.buckets;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedList;

public class ImmutableLinkedList<T> {
    private final LinkedList<T> list;

    public ImmutableLinkedList(){
        list = new LinkedList<>();
    }

    public ImmutableLinkedList(LinkedList<T> list){
        this.list = list;
    }

    public ImmutableLinkedList<T> add(T element){
        LinkedList<T> newList = new LinkedList<>(list);
        newList.add(element);
        return new ImmutableLinkedList<>(newList);
    }

    public int size() {
        return list.size();
    }

    public SimpleImmutableEntry<ImmutableLinkedList<T>, T> poll() {
        LinkedList<T> newList = new LinkedList<>(list);
        T element = newList.poll();
        ImmutableLinkedList<T> newImmutableLinkedList = new ImmutableLinkedList<>(newList);
        return new SimpleImmutableEntry<>(newImmutableLinkedList, element);
    }
}
