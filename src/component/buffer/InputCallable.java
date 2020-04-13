package component.buffer;

public interface InputCallable<K> extends ComponentCallable {

    void call(K input);

    default InputCallable<K> toSequential(){
        return new InputCallable<>(){

            @Override
            public void call(K input) {
                InputCallable.this.call(input);
            }

        };
    }

    default InputCallable<K> toParallel(){
        return new InputCallable<>(){

            @Override
            public void call(K input) {
                InputCallable.this.call(input);
            }

        };
    }

}
