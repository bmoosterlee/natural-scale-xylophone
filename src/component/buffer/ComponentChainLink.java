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

    protected abstract void componentTick();

    public void breakChain(){
        tryToBreakSequentialChain();
        tryToBreakParallelChain();
    }

    private void tryToBreakSequentialChain() {
        if(!isParallelisable()){
            new TickRunningStrategy(sequentialWrap());
        }
        else if(previousSequentialComponentChainLink!=null) {
            new TickRunningStrategy(previousSequentialComponentChainLink.sequentialWrap());
        }
    }

    protected abstract <K, V> AbstractComponent<K, V> sequentialWrap();

    void sequentialAwareTick() {
        if(previousSequentialComponentChainLink!=null) {
            previousSequentialComponentChainLink.sequentialAwareTick();
        }

        componentTick();
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
        if(previousComponentChainLink!=null){
            if(previousComponentChainLink.isParallelisable()) {
                previousComponentChainLink.parallelisationAwareTick();
            }
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
