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

    static <K, V> void parallelChainCheck(BufferChainLink<K> inputBuffer, AbstractComponent<K, V> component) {
        if(inputBuffer.previousComponent.isParallelisable() && !component.isParallelisable()){
            new TickRunningStrategy(new AbstractPipeComponent(inputBuffer.previousComponent.getParallisationAwareFirstInputPort(), inputBuffer.previousComponent.getOutputPort()) {
                @Override
                protected void tick() {
                    inputBuffer.previousComponent.parallelisationAwareTick();
                }
            }, true);
        }
    }

    abstract InputPort getFirstInputPort();

    abstract Boolean isParallelisable();

    protected abstract OutputPort getOutputPort();
}
