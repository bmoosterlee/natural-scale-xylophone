package component.buffer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public abstract class TickRunnerSpawner extends TickRunner{
    private final Collection<? extends BoundedBuffer> inputBuffers;
    private final Collection<? extends BoundedBuffer> outputBuffers;
    private final LinkedList<SimpleTickRunner> liveRunners;

    public <K extends BoundedBuffer, V extends BoundedBuffer> TickRunnerSpawner(Collection<K> inputBuffers, Collection<V> outputBuffers){
        this.inputBuffers = inputBuffers;
        this.outputBuffers = outputBuffers;
        liveRunners = new LinkedList<>();
    }

    public <K extends BoundedBuffer, V extends BoundedBuffer> TickRunnerSpawner(K inputBuffer, V outputBuffer){
        this(Collections.singleton(inputBuffer), Collections.singleton(outputBuffer));
    }

    public <K extends BoundedBuffer, V extends BoundedBuffer> TickRunnerSpawner(Collection<K> inputBuffers, V outputBuffer){
        this(inputBuffers, Collections.singleton(outputBuffer));
    }

    public <K extends BoundedBuffer, V extends BoundedBuffer> TickRunnerSpawner(K inputBuffer, Collection<V> outputBuffers){
        this(Collections.singleton(inputBuffer), outputBuffers);
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
