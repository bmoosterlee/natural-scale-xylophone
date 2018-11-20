package component.buffer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

public abstract class TickRunnerSpawner extends TickRunner{
    private final Collection<BoundedBuffer> inputBuffers;
    private final Collection<BoundedBuffer> outputBuffers;
    private final LinkedList<SimpleTickRunner> liveRunners;

    public <K extends BoundedBuffer, V extends BoundedBuffer> TickRunnerSpawner(AbstractComponent<K, V> component){
        this.inputBuffers = new HashSet<>();
        for(InputPort<K> inputPort : component.getInputPorts()){
            this.inputBuffers.add(inputPort.getBuffer());
        }
        this.outputBuffers = new HashSet<>();
        for(OutputPort<V> outputPort : component.getOutputPorts()){
            this.outputBuffers.add(outputPort.getBuffer());
        }
        liveRunners = new LinkedList<>();
    }

    @Override
    public void run() {
        SimpleTickRunner firstTickRunner = createNewTickRunner();
        firstTickRunner.start();
        liveRunners.add(firstTickRunner);

        while(true){
            if(!anyClog(outputBuffers)){
                if(anyClog(inputBuffers)) {
                    SimpleTickRunner tickRunner = createNewTickRunner();
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

    private SimpleTickRunner createNewTickRunner() {
        return new SimpleTickRunner() {

            @Override
            protected void tick() {
                TickRunnerSpawner.this.tick();
            }

        };
    }

}
