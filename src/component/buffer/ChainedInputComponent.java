package component.buffer;

public class ChainedInputComponent<K> extends MethodInputComponent<K> {

    private final ChainedPipeComponent previousComponent;

    public ChainedInputComponent(BufferChainLink<K> inputBuffer, CallableWithArgument<K> method){
        super(inputBuffer, method);
        previousComponent = inputBuffer.previousComponent;
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

    public <T> AbstractInputComponent<T> wrap() {
        return new AbstractInputComponent<>(getFirstInputPort()) {

            @Override
            protected void tick() {
                ChainedInputComponent.this.tick();
            }
        };
    }

    private <T> InputPort<T> getFirstInputPort() {
        ChainedPipeComponent index = this.previousComponent;
        while(index.previousComponent!=null){
            index = index.previousComponent;
        }
        return index.input;
    }

}
