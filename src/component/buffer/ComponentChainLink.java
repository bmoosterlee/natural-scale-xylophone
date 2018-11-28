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
            new TickRunningStrategy(new SequentialChain<>(sequentialChainLinks));
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

    static <K, V> void tryToBreakParallelChain(BufferChainLink<K> previousComponentOutputBuffer, AbstractComponent<K, V> newComponent) {
        if(!newComponent.isParallelisable()) {
            previousComponentOutputBuffer.previousComponent.tryToBreakParallelChain();
        }
    }

    private void tryToBreakParallelChain() {
        if (isParallelisable()) {
            new TickRunningStrategy(parallelWrap());
        }
    }

    protected abstract <K, V> AbstractComponent<K, V> parallelWrap();

    void parallelisationAwareTick() {
        if(previousComponentChainLink!=null){
            if(previousComponentChainLink.isParallelisable()) {
                previousComponentChainLink.parallelisationAwareTick();
            }
        }

        componentTick();
    }

    InputPort getParallelisationAwareFirstInputPort() {
        try{
            if(previousComponentChainLink.isParallelisable()) {
                InputPort parallelisationAwareFirstInputPort = previousComponentChainLink.getParallelisationAwareFirstInputPort();
                if(parallelisationAwareFirstInputPort!=null) {
                    return parallelisationAwareFirstInputPort;
                }
            }
        }
        catch(NullPointerException ignored) {
        }

        return getInputPort();
    }

    abstract Boolean isParallelisable();

    protected abstract InputPort getInputPort();

    protected abstract OutputPort getOutputPort();
}
