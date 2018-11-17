package component.buffer;

import java.util.LinkedList;

public abstract class TickRunnerSpawner implements Runnable{
    private final SimpleBuffer inputBuffer;
    private final LinkedList<TickRunner> liveRunners;

    public TickRunnerSpawner(SimpleBuffer inputBuffer){
        this.inputBuffer = inputBuffer;
        liveRunners = new LinkedList<>();
    }

    public void start(){
        new Thread(this).start();
    }

    @Override
    public void run() {
        TickRunner firstTickRunner = createNewTickRunner();
        firstTickRunner.start();
        liveRunners.add(firstTickRunner);

        while(true){
            if(inputBuffer.isFull()){
                TickRunner tickRunner = createNewTickRunner();
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
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private TickRunner createNewTickRunner() {
        return new TickRunner() {

                        @Override
                        protected void tick() {
                            TickRunnerSpawner.this.tick();
                        }

                    };
    }

    protected abstract void tick();

}
