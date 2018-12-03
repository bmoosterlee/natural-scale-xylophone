package component.buffer;

public class TickRunningStrategy {

    public TickRunningStrategy(final AbstractComponent pipeComponent, int maxThreadCount){
        if(pipeComponent.isParallelisable()){
            new TickRunnerSpawner(pipeComponent, maxThreadCount).start();
        }
        else {
            new SimpleTickRunner(pipeComponent).start();
        }
    }

    public TickRunningStrategy(final AbstractComponent pipeComponent){
        if(pipeComponent.isParallelisable()){
            new TickRunnerSpawner(pipeComponent).start();
        }
        else {
            new SimpleTickRunner(pipeComponent).start();
        }
    }

}
