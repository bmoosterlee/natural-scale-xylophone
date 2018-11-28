package component.buffer;

import java.util.LinkedList;

public class SequentialChain<K, V> extends ParallelisabilityChain<K, V> {

    public SequentialChain(LinkedList<ComponentChainLink> chainLinks) {
        super(chainLinks, () -> {
            for(ComponentChainLink chainLink : chainLinks){
                synchronized (chainLink) {
                    chainLink.componentTick();
                }
            }
            return null;
        });
    }

}
