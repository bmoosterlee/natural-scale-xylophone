package component.buffer;

public class RunningPipeComponent<K, V> extends PipeComponent<K, V> {

    private final MyTickRunner tickRunner = new MyTickRunner();

    public RunningPipeComponent(SimpleBuffer<K> inputBuffer, BoundedBuffer<V> outputBuffer, CallableWithArguments<K, V> method){
        super(inputBuffer, outputBuffer, method);

        start();
    }

    private class MyTickRunner extends SimpleTickRunner {

        @Override
        protected void tick() {
            RunningPipeComponent.this.tick();
        }

    }

    protected void start() {
        tickRunner.start();
    }

}
