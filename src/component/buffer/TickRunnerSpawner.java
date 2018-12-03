package component.buffer;

import java.util.Collection;
import java.util.LinkedList;

public class TickRunnerSpawner extends TickRunner{
    private final Collection<BoundedBuffer> inputBuffers;
    private final Collection<BoundedBuffer> outputBuffers;
    private final LinkedList<SimpleTickRunner> liveRunners;
    private final AbstractComponent component;
    private final int minimumThreadCount;
    private final int maxThreadCount;

    public TickRunnerSpawner(AbstractComponent component, int maxThreadCount){
        this.maxThreadCount = maxThreadCount;

        this.component = component;
        this.inputBuffers = component.getInputBuffers();
        this.outputBuffers = component.getOutputBuffers();

        liveRunners = new LinkedList<>();
        minimumThreadCount = 1;

        add();
    }

    public TickRunnerSpawner(AbstractComponent component){
        this(component, Integer.MAX_VALUE);
    }

    @Override
    protected void tick() {
        if(!anyClog(outputBuffers)){
            if(anyClog(inputBuffers)) {
                add();
            }
            else{
                tryRemove();
            }
        }
        else {
//            if (allEmpty(inputBuffers)) {
                tryRemove();
//            }
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void add() {
        if(liveRunners.size() < maxThreadCount) {
            SimpleTickRunner firstTickRunner = new SimpleTickRunner(component);
            firstTickRunner.start();
            liveRunners.add(firstTickRunner);
        }
    }

    private void tryRemove() {
        if (liveRunners.size() > minimumThreadCount) {
            try {
                liveRunners.remove().kill();
            } catch (NullPointerException ignored) {
            }
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
