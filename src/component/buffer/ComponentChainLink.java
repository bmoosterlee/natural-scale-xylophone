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
            parallelChainCheck(previousComponentOutputBuffer.previousComponent);
        }
    }

    static <K> void parallelChainCheck(final ComponentChainLink previousComponent) {
        if (previousComponent.isParallelisable()) {
            new TickRunningStrategy(previousComponent.parallelWrap(), true);
        }
    }

    protected abstract AbstractComponent parallelWrap();

    protected abstract void parallelisationAwareTick();

    abstract InputPort getParallelisationAwareFirstInputPort();

    public abstract <T, V> AbstractComponent<T, V> wrap();

    abstract InputPort getFirstInputPort();

    abstract Boolean isParallelisable();

    protected abstract OutputPort getOutputPort();
}
