package component.buffer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TickRunnerSpawner extends TickRunner{
    private final Collection<BoundedBuffer> inputBuffers;
    private final Collection<BoundedBuffer> outputBuffers;
    final LinkedList<SimpleTickRunner> liveRunners;
    private final AbstractComponent component;
    private final int minimumThreadCount;
    private final int maxThreadCount;
    private final int timeTillNextCheck = 1000;

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
        if(anyTopClog(inputBuffers) && anySignificantClog(inputBuffers)) {
            add();
            add();
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

    static boolean anyClog(Collection<? extends BoundedBuffer> buffers) {
        for (BoundedBuffer buffer : buffers) {
            if (buffer.isFull()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyTopClog(Collection<? extends BoundedBuffer> buffers) {
        for (BoundedBuffer buffer : buffers) {
            if (TrafficAnalyzer.trafficAnalyzer.topClogs.keySet().contains(buffer.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean anySignificantClog(Collection<BoundedBuffer> buffers) {
        Map<String, AtomicInteger> topClogs = TrafficAnalyzer.trafficAnalyzer.topClogs;
        for (BoundedBuffer buffer : buffers) {
            if (topClogs.keySet().contains(buffer.getName()) && topClogs.get(buffer.getName()).get()>=buffer.getSize()) {
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
