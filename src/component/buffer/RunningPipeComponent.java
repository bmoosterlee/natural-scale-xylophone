package component.buffer;

public class RunningPipeComponent<K, V> {

    private final PipeComponent<K, V> pipeComponent;

    public RunningPipeComponent(final PipeComponent<K, V> pipeComponent){
        this.pipeComponent = pipeComponent;

        new SimpleTickRunner() {
            @Override
            protected void tick() {
                RunningPipeComponent.this.tick();
            }
        }.start();
    }

    private void tick() {
        pipeComponent.tick();
    }

}
