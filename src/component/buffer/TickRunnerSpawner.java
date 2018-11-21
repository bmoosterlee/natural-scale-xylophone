package component.buffer;

import java.util.Collection;
import java.util.LinkedList;

public class TickRunnerSpawner<K, V> extends TickRunner{
    private final Collection<BoundedBuffer<K>> inputBuffers;
    private final Collection<BoundedBuffer<V>> outputBuffers;
    private final LinkedList<SimpleTickRunner> liveRunners;
    private final AbstractComponent component;
    private final int minimumThreadCount;

    public TickRunnerSpawner(AbstractComponent<K, V> component, boolean threadAlreadyRunning){
        this.component = component;
        this.inputBuffers = component.getInputBuffers();
        this.outputBuffers = component.getOutputBuffers();

        liveRunners = new LinkedList<>();
        if(threadAlreadyRunning){
            minimumThreadCount = 0;
        }
        else{
            minimumThreadCount = 1;
        }

        SimpleTickRunner firstTickRunner = new SimpleTickRunner(component);
        firstTickRunner.start();
        liveRunners.add(firstTickRunner);
    }

    @Override
    protected void tick() {
        if(!anyClog(outputBuffers)){
            if(anyClog(inputBuffers)) {
                SimpleTickRunner tickRunner = new SimpleTickRunner(component);
                tickRunner.start();
                liveRunners.add(tickRunner);
            }
        }
        else {
            if (allEmpty(inputBuffers)) {
                if (liveRunners.size() > minimumThreadCount) {
                    try {
                        liveRunners.remove().kill();
                    } catch (NullPointerException ignored) {
                    }
                }
            }
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean anyClog(Collection<? extends BoundedBuffer> buffers) {
        boolean anyClog = false;
        for (BoundedBuffer buffer : buffers) {
            if (buffer.isFull()) {
                anyClog = true;
                break;
            }
        }
        return anyClog;
    }

    private boolean allEmpty(Collection<? extends BoundedBuffer> buffers) {
        boolean allEmpty = true;
        for(BoundedBuffer buffer : buffers){
            if(!buffer.isEmpty()){
                allEmpty = false;
                break;
            }
        }
        return allEmpty;
    }

}
