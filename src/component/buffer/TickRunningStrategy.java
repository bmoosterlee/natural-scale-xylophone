package component.buffer;

public class TickRunningStrategy {

    public <K, V> TickRunningStrategy(final AbstractComponent<K, V> pipeComponent){
        if(pipeComponent.isParallelisable()){
            new TickRunnerSpawner<>(pipeComponent).start();
        }
        else {
            new SimpleTickRunner(pipeComponent).start();
        }
    }

}
