package component.buffer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

public class TickRunnerSpawner extends TickRunner{
    private final Collection<BoundedBuffer> inputBuffers;
    private final Collection<BoundedBuffer> outputBuffers;
    private final LinkedList<SimpleTickRunner> liveRunners;
    private final AbstractComponent component;
    private final int minimumThreadCount;
    private final int maxThreadCount;
    private final int timeTillNextCheck = 100;

    public TickRunnerSpawner(AbstractComponent component, int maxThreadCount){
        this.maxThreadCount = maxThreadCount;

        this.component = component;
        this.inputBuffers = component.getInputBuffers();
        this.outputBuffers = component.getOutputBuffers();

        liveRunners = new LinkedList<>();
        minimumThreadCount = 1;

        try {
            Thread.sleep(new Random().nextInt(timeTillNextCheck));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        add();
    }

    public TickRunnerSpawner(AbstractComponent component){
        this(component, Integer.MAX_VALUE);
    }

    @Override
    protected void tick() {
        if(!anyClog(outputBuffers)){
            if(!allEmpty(inputBuffers)) {
                add();
            }
        } else {
            tryRemove();
        }

        try {
            Thread.sleep(timeTillNextCheck);
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
        for (BoundedBuffer buffer : buffers) {
            if (buffer.isFull()) {
                return true;
            }
        }
        return false;
    }

    private boolean allEmpty(Collection<? extends BoundedBuffer> buffers) {
        for(BoundedBuffer buffer : buffers){
            if(!buffer.isEmpty()){
                return false;
            }
        }
        return true;
    }

}
