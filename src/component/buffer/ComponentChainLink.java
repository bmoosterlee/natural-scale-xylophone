package component.buffer;

public abstract class ComponentChainLink<K> {
    final PipeComponentChainLink<?, K> previousComponentChainLink;

    public ComponentChainLink(PipeComponentChainLink<?, K> previousComponentChainLink) {
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
}
