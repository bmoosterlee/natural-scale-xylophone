package component.buffer;

import java.util.LinkedList;

public class SequentialChain<K, V> extends ParallelisabilityChain<K, V> {

    public SequentialChain(LinkedList<ComponentChainLink> chainLinks) {
        super(chainLinks, () -> {
            for(ComponentChainLink chainLink : chainLinks){
                chainLink.componentTick();
            }
            return null;
        });
    }

    @Override
    public Boolean isParallelisable(){
        return false;
    }

}
