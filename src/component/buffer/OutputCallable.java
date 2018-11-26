package component.buffer;

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

}
