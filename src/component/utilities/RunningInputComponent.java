package component.utilities;

import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArgument;

public class RunningInputComponent<K> extends InputComponent<K> {

    private final MyTickRunner tickRunner = new MyTickRunner();

    public RunningInputComponent(BoundedBuffer<K> inputBuffer, CallableWithArgument<K> method){
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
