package component.buffer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Callable;

public abstract class ParallelisabilityChain<K, V> extends AbstractComponent<K, V> {
    protected final Callable tickMethod;
    protected final Collection<BoundedBuffer<K>> inputBuffers;
    protected final Collection<BoundedBuffer<V>> outputBuffers;

    public ParallelisabilityChain(LinkedList<ComponentChainLink> chainLinks, Callable tickMethod) {
        this.tickMethod = tickMethod;

        InputPort inputPort = chainLinks.getFirst().getInputPort();
        if(inputPort==null){
            if(chainLinks.size()==1){
                inputBuffers = Collections.emptyList();
            }
            else{
                inputBuffers = Collections.singleton(chainLinks.get(1).getInputPort().getBuffer());
            }
        }
        else{
            inputBuffers = Collections.singleton(inputPort.getBuffer());
        }

        OutputPort outputPort = chainLinks.getLast().getOutputPort();
        if(outputPort==null) {
            outputBuffers = Collections.emptyList();
        }
        else{
            outputBuffers = Collections.singleton(outputPort.getBuffer());
        }
    }

    @Override
    protected Collection<BoundedBuffer<K>> getInputBuffers() {
        return inputBuffers;
    }

    @Override
    protected Collection<BoundedBuffer<V>> getOutputBuffers() {
        return outputBuffers;
    }

    @Override
    protected void tick() {
        try {
            tickMethod.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
