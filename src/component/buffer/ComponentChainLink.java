package component.buffer;

public abstract class ComponentChainLink {
    private final ComponentChainLink previousComponentChainLink;
    private final ComponentChainLink previousSequentialComponentChainLink;

    ComponentChainLink(ComponentChainLink previousComponentChainLink) {
        this.previousComponentChainLink = previousComponentChainLink;

        ComponentChainLink previousSequentialComponentChainLink1;
        try {
            if (!previousComponentChainLink.isParallelisable()) {
                previousSequentialComponentChainLink1 = previousComponentChainLink;
            }
            else{
                previousSequentialComponentChainLink1 = previousComponentChainLink.previousSequentialComponentChainLink;
            }
        }
        catch(NullPointerException ignored){
            previousSequentialComponentChainLink1 = null;
        }

        previousSequentialComponentChainLink = previousSequentialComponentChainLink1;
    }

    void sequentialAwareTick() {
        try{
            previousSequentialComponentChainLink.sequentialAwareTick();
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
        if(!isParallelisable()){
            new TickRunningStrategy(sequentialWrap());
        }
        else if(previousSequentialComponentChainLink!=null) {
            new TickRunningStrategy(previousSequentialComponentChainLink.sequentialWrap());
        }
    }

    InputPort getSequentialAwareFirstInputPort() {
        try{
            InputPort sequentialAwareFirstInputPort = previousSequentialComponentChainLink.getSequentialAwareFirstInputPort();
            if(sequentialAwareFirstInputPort!=null) {
                return sequentialAwareFirstInputPort;
            }
        }
        catch(NullPointerException ignored) {
        }

        return getInputPort();
    }

    protected abstract <K, V> AbstractComponent<K, V> sequentialWrap();

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

    abstract Boolean isParallelisable();

    protected abstract InputPort getInputPort();

    protected abstract OutputPort getOutputPort();
}
