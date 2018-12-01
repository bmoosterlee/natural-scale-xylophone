package component.buffer;

public interface PipeCallable<K, V> extends ComponentCallable {

    V call(K input);

    default PipeCallable<K, V> toSequential(){
        return new PipeCallable<>(){

            @Override
            public V call(K input) {
                return PipeCallable.this.call(input);
            }

            @Override
            public Boolean isParallelisable() {
                return false;
            }
        };
    }

    default PipeCallable<K, V> toParallel(){
        return new PipeCallable<>(){

            @Override
            public V call(K input) {
                return PipeCallable.this.call(input);
            }

            @Override
            public Boolean isParallelisable() {
                return true;
            }
        };
    }

    default <W> PipeCallable<K, W> chainTo(PipeCallable<V, W> nextMethod){
        return input -> nextMethod.call(PipeCallable.this.call(input));
    }

    default InputCallable<K> chainTo(InputCallable<V> nextMethod){
        return input -> nextMethod.call(PipeCallable.this.call(input));
    }

}
