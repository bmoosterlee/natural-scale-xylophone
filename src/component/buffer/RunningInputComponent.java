package component.buffer;

public class RunningInputComponent<K> {

    private final InputComponent<K> inputComponent;

    public RunningInputComponent(final InputComponent<K> inputComponent){
        this.inputComponent = inputComponent;

        new SimpleTickRunner() {
            @Override
            protected void tick() {
                RunningInputComponent.this.tick();
            }
        }.start();
    }

    private void tick() {
        inputComponent.tick();
    }

}
