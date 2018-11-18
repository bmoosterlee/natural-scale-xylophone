package component.buffer;

public class ChainedInputComponent<K> extends InputComponent<K> {

    private final ChainedPipeComponent previousComponent;

    public ChainedInputComponent(BufferChainLink<K> inputBuffer, CallableWithArgument<K> method){
        super(inputBuffer, method);
        previousComponent = inputBuffer.previousComponent;
        start();
    }

    public void start() {
        new SimpleTickRunner() {

            @Override
            protected void tick() {
                ChainedInputComponent.this.tick();
            }

        }
        .start();
    }

    @Override
    protected void tick() {
        try{
            previousComponent.tick();
        }
        catch(NullPointerException ignored){
        }
        super.tick();
    }

}
