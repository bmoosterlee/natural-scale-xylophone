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
            parallelisationChainCheck(previousComponentOutputBuffer.previousComponent);
        }
    }

    private static <K> void parallelisationChainCheck(final ComponentChainLink previousComponent) {
        if (previousComponent.isParallelisable()) {
            new TickRunningStrategy(new AbstractPipeComponent<>(previousComponent.getParallelisationAwareFirstInputPort(), previousComponent.getOutputPort()) {
                @Override
                protected void tick() {
                    previousComponent.parallelisationAwareTick();
                }
            }, true);
        }
    }

    protected abstract void parallelisationAwareTick();

    abstract InputPort getParallelisationAwareFirstInputPort();

    public abstract <T, V> AbstractComponent<T, V> wrap();

    abstract InputPort getFirstInputPort();

    abstract Boolean isParallelisable();

    protected abstract OutputPort getOutputPort();
}
