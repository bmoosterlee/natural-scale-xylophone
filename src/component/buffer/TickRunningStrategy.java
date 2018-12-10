package component.buffer;

public class TickRunningStrategy {

    public TickRunningStrategy(final AbstractComponent component, int maxThreadCount){
        if(component.isParallelisable()){
            new TickRunnerSpawner(component, maxThreadCount).start();
        }
        else {
            new SimpleTickRunner(component).start();
        }
    }

    public TickRunningStrategy(final AbstractComponent component){
        if(component.isParallelisable()){
            new TickRunnerSpawner(component).start();
        }
        else {
            new SimpleTickRunner(component).start();
        }
    }

}
