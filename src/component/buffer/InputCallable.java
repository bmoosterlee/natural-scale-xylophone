package component.buffer;

public interface InputCallable<K> extends ComponentCallable {

    void call(K input);

}
