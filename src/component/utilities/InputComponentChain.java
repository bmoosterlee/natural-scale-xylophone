package component.utilities;

import component.buffer.BufferChainLink;
import component.buffer.CallableWithArgument;

public class InputComponentChain<K> extends InputComponent<K> {

    private final PipeComponentChain previousComponent;

    public InputComponentChain(BufferChainLink<K> inputBuffer, CallableWithArgument<K> method){
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

}
