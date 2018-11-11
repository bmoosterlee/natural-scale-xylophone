package component;

public class TickableInputComponent<K> extends Tickable {

    protected final InputPort<K> input;
    private final CallableWithArgument<K> method;

    public TickableInputComponent(BoundedBuffer<K> inputBuffer, CallableWithArgument<K> method){
        this.method = method;

        input = new InputPort<>(inputBuffer);

        start();
    }

    @Override
    protected void tick() {
        try {
            K consumed = input.consume();
            method.call(consumed);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
