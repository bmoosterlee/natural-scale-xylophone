package component.buffer;

public class RunningInputComponent<K> extends InputComponent<K> {

    private final MyTickRunner tickRunner = new MyTickRunner();

    public RunningInputComponent(SimpleBuffer<K> inputBuffer, CallableWithArgument<K> method){
        super(inputBuffer, method);

        start();
    }

    private class MyTickRunner extends TickRunner {
        @Override
        protected void tick() {
            RunningInputComponent.this.tick();
        }
    }

    protected void start() {
        tickRunner.start();
    }

}
