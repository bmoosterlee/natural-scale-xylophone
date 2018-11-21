package component.buffer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.Callable;

public class TickRunnerSpawner extends TickRunner{
    private final Collection<BoundedBuffer> inputBuffers;
    private final Collection<BoundedBuffer> outputBuffers;
    private final LinkedList<SimpleTickRunner> liveRunners;
    private final AbstractComponent component;

    public <K extends BoundedBuffer, V extends BoundedBuffer> TickRunnerSpawner(AbstractComponent<K, V> component){
        this.component = component;

        this.inputBuffers = new HashSet<>();
        for(InputPort<K> inputPort : component.getInputPorts()){
            this.inputBuffers.add(inputPort.getBuffer());
        }
        this.outputBuffers = new HashSet<>();
        for(OutputPort<V> outputPort : component.getOutputPorts()){
            this.outputBuffers.add(outputPort.getBuffer());
        }
        liveRunners = new LinkedList<>();

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
                if (liveRunners.size() > 1) {
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
