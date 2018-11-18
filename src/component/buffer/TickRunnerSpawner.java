package component.buffer;

import java.util.LinkedList;

public abstract class TickRunnerSpawner extends TickRunner{
    private final BoundedBuffer inputBuffer;
    private final LinkedList<SimpleTickRunner> liveRunners;

    public TickRunnerSpawner(BoundedBuffer inputBuffer){
        this.inputBuffer = inputBuffer;
        liveRunners = new LinkedList<>();
    }

    @Override
    public void run() {
        SimpleTickRunner firstTickRunner = createNewTickRunner();
        firstTickRunner.start();
        liveRunners.add(firstTickRunner);

        while(true){
            if(inputBuffer.isFull()){
                SimpleTickRunner tickRunner = createNewTickRunner();
                tickRunner.start();
                liveRunners.add(tickRunner);
            }
            else if(inputBuffer.isEmpty()){
                if(liveRunners.size()>1) {
                    try {
                        liveRunners.remove().kill();
                    } catch (NullPointerException ignored) {
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

    private SimpleTickRunner createNewTickRunner() {
        return new SimpleTickRunner() {

            @Override
            protected void tick() {
                TickRunnerSpawner.this.tick();
            }

        };
    }

}
