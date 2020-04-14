package component.buffer;

public interface PipeCallable<K, V> extends ComponentCallable {

    V call(K input);

    default <W> PipeCallable<K, W> chainTo(PipeCallable<V, W> nextMethod){
        return input -> nextMethod.call(PipeCallable.this.call(input));
    }

    default InputCallable<K> chainTo(InputCallable<V> nextMethod){
        return input -> nextMethod.call(PipeCallable.this.call(input));
    }

}
