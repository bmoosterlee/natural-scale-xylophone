package component.utilities;

import component.buffer.BufferChainLink;
import component.buffer.CallableWithArgument;

public class TickableInputComponentChain<K> extends TickableInputComponent {

    private final TickablePipeComponentChain previousComponent;

    public TickableInputComponentChain(BufferChainLink<K> inputBuffer, CallableWithArgument<K> method){
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
    }

}
