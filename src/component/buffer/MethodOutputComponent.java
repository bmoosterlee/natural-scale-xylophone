package component.buffer;

import main.OutputCallable;

public class MethodOutputComponent<V> extends AbstractOutputComponent<V> {
    protected final OutputCallable<V> method;
    private final Boolean parallelisability;

    public MethodOutputComponent(SimpleBuffer<V> outputBuffer, OutputCallable<V> method) {
        super(new OutputPort<>(outputBuffer));
        this.method = method;
        parallelisability = method.isParallelisable();
    }

    public void tick() {
        try {
            V result = method.call();
            output.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Boolean isParallelisable(){
        return parallelisability;
    }

}
