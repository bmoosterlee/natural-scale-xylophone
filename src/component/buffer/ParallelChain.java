package component.buffer;

import java.util.LinkedList;

public class ParallelChain<K, V> extends ParallelisabilityChain<K, V> {

    public ParallelChain(LinkedList<ComponentChainLink> chainLinks) {
        super(chainLinks, () -> {
            for(ComponentChainLink chainLink : chainLinks){
                chainLink.componentTick();
            }
            return null;
        });
    }

}
