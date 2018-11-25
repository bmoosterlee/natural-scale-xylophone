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

    static <K, V> void parallelChainCheck(BufferChainLink<K> previousComponentOutputBuffer, AbstractComponent<K, V> newComponent) {
        if(!newComponent.isParallelisable()) {
            previousComponentOutputBuffer.previousComponent.parallelChainCheck();
        }
    }

    void parallelChainCheck() {
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

    abstract InputPort getParallelisationAwareFirstInputPort();

    public abstract <T, V> AbstractComponent<T, V> wrap();

    abstract InputPort getFirstInputPort();

    abstract Boolean isParallelisable();

    protected abstract OutputPort getOutputPort();
}
