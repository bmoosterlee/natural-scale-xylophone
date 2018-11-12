package component.utilities;

import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArgument;
import component.buffer.InputPort;

public class TickableInputComponent<K> extends InputComponent<K> {

    private final MyTickable tickable = new MyTickable();

    public TickableInputComponent(BoundedBuffer<K> inputBuffer, CallableWithArgument<K> method){
        super(inputBuffer, method);

        start();
    }

    private class MyTickable extends Tickable {
        @Override
        protected void tick() {
            TickableInputComponent.this.tick();
        }
    }

    protected void start() {
        tickable.start();
    }

}
