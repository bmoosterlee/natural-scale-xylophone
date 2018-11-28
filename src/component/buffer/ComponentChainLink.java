package component.buffer;

import java.util.LinkedList;

public abstract class ComponentChainLink {
    private final ComponentChainLink previousComponentChainLink;

    ComponentChainLink(ComponentChainLink previousComponentChainLink) {
        this.previousComponentChainLink = previousComponentChainLink;
    }

    protected abstract void componentTick();

    public void breakChain(){
        tryToBreakSequentialChain();
        tryToBreakParallelChain();
    }

    private void tryToBreakSequentialChain() {
        LinkedList<ComponentChainLink> sequentialChainLinks = listChainLinksByParallelisability(false);
        if(!sequentialChainLinks.isEmpty()){
            new TickRunningStrategy(new SequentialChain<>(sequentialChainLinks), sequentialChainLinks.size());
        }
    }

    private void tryToBreakParallelChain() {
        LinkedList<ComponentChainLink> parallelChainLinks = listChainLinksByParallelisability(true);
        if(!parallelChainLinks.isEmpty()){
            new TickRunningStrategy(new ParallelChain<>(parallelChainLinks));
        }
    }

    private LinkedList<ComponentChainLink> listChainLinksByParallelisability(boolean parallelisability) {
        LinkedList<ComponentChainLink> accumulator = new LinkedList<>();
        ComponentChainLink index = this;
        while(index!=null){
            if(index.isParallelisable() == parallelisability) {
                accumulator.addFirst(index);
            }
            index = index.previousComponentChainLink;
        }
        return accumulator;
    }

    abstract Boolean isParallelisable();

    protected abstract InputPort getInputPort();

    protected abstract OutputPort getOutputPort();
}
