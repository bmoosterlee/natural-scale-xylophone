package notes.state;

public interface Observer<T> {

    public void notify(T event);

}
