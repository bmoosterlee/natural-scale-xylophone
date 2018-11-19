package component.buffer;

public class ChainedInputComponent<K> {
    private final MethodInputComponent<K> methodInputComponent;
    private final ChainedPipeComponent previousComponent;

    public ChainedInputComponent(ChainedPipeComponent<?, K> previousComponent, MethodInputComponent<K> methodInputComponent){
        this.methodInputComponent = methodInputComponent;
        this.previousComponent = previousComponent;
    }

    public void tick() {
        try{
            previousComponent.tick();
        }
        catch(NullPointerException ignored){
        }
        methodInputComponent.tick();
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
        return index.methodPipeComponent.input;
    }

}
