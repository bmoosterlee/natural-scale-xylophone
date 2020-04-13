package component.buffer;

public abstract class ComponentChainLink<K, V, A extends Packet<K>, B extends Packet<V>> {
    protected final ComponentChainLink<?, K, ?, A> previousComponentChainLink;

    ComponentChainLink(ComponentChainLink<?, K, ?, A> previousComponentChainLink) {
        this.previousComponentChainLink = previousComponentChainLink;
    }

    public void breakChain(){
        wrap();
    }

    protected abstract void wrap();

    protected abstract <W, Y extends Packet<W>> void wrap(PipeCallable<V, W> nextMethod, SimpleBuffer<W, Y> outputBuffer, int chainLinks);

    protected abstract void wrap(InputCallable<V> nextMethod, int chainLinks);

}
