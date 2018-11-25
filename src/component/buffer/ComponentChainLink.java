package component.buffer;

public abstract class ComponentChainLink {
    final ComponentChainLink previousComponentChainLink;

    public ComponentChainLink(ComponentChainLink previousComponentChainLink) {
        this.previousComponentChainLink = previousComponentChainLink;
    }

    protected void tick() {
        try{
            previousComponentChainLink.tick();
        }
        catch(NullPointerException ignored){
        }

        componentTick();
    }

    protected abstract void componentTick();

    public void breakChain(){
        tryToBreakParallelChain();
        tryToBreakSequentialChain();
    }

    private void tryToBreakSequentialChain() {
        new TickRunningStrategy(sequentialWrap(), false);
    }

    protected abstract <K, V> AbstractComponent<K, V> sequentialWrap();

    static <K, V> void tryToBreakParallelChain(BufferChainLink<K> previousComponentOutputBuffer, AbstractComponent<K, V> newComponent) {
        if(!newComponent.isParallelisable()) {
            previousComponentOutputBuffer.previousComponent.tryToBreakParallelChain();
        }
    }

    void tryToBreakParallelChain() {
        if (isParallelisable()) {
            new TickRunningStrategy(parallelWrap(), true);
        }
    }

    protected abstract <K, V> AbstractComponent<K, V> parallelWrap();

    protected void parallelisationAwareTick() {
        try{
            if(previousComponentChainLink.isParallelisable()) {
                previousComponentChainLink.parallelisationAwareTick();
            }
        }
        catch(NullPointerException ignored){
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

    abstract InputPort getFirstInputPort();

    abstract Boolean isParallelisable();

    protected abstract InputPort getInputPort();

    protected abstract OutputPort getOutputPort();
}
