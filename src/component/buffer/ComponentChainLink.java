package component.buffer;

public abstract class ComponentChainLink<K, V> {
    protected final ComponentChainLink<?, K> previousComponentChainLink;

    ComponentChainLink(ComponentChainLink<?, K> previousComponentChainLink) {
        this.previousComponentChainLink = previousComponentChainLink;
    }

    protected abstract void componentTick();

    public void breakChain(){
        wrap();
    }

    protected abstract void wrap();

    protected abstract void wrap(PipeCallable<V, ?> nextMethod, BoundedBuffer outputBuffer, int chainLinks);

    protected abstract void wrap(InputCallable<V> nextMethod, BoundedBuffer outputBuffer, int chainLinks);

    abstract Boolean isParallelisable();

    protected abstract InputPort getInputPort();

    protected abstract OutputPort getOutputPort();
}
