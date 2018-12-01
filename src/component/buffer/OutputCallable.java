package component.buffer;

import java.util.concurrent.Callable;

public interface OutputCallable<V> extends ComponentCallable {

    V call();

    default OutputCallable<V> toSequential(){
        return new OutputCallable<>(){

            @Override
            public V call() {
                return OutputCallable.this.call();
            }

            @Override
            public Boolean isParallelisable() {
                return false;
            }
        };
    }

    default OutputCallable<V> toParallel(){
        return new OutputCallable<>(){

            @Override
            public V call() {
                return OutputCallable.this.call();
            }

            @Override
            public Boolean isParallelisable() {
                return true;
            }
        };
    }

    default <W> OutputCallable<W> chainTo(PipeCallable<V, W> nextMethod){
        return () -> nextMethod.call(OutputCallable.this.call());
    }

    default Callable<Void> chainTo(InputCallable<V> nextMethod){
        return () -> {
            nextMethod.call(OutputCallable.this.call());
            return null;
        };
    }

}
