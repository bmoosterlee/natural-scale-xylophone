package component.buffer;

public abstract class ComponentChainLink<K, V> {
    protected final ComponentChainLink<?, K> previousComponentChainLink;

    ComponentChainLink(ComponentChainLink<?, K> previousComponentChainLink) {
        this.previousComponentChainLink = previousComponentChainLink;
    }

    public void breakChain(){
        wrap();
    }

    protected abstract void wrap();

    protected abstract <W> void wrap(PipeCallable<V, W> nextMethod, BoundedBuffer<W> outputBuffer, int chainLinks);

    protected abstract <W> void wrap(InputCallable<V> nextMethod, int chainLinks);

    abstract Boolean isParallelisable();

}
