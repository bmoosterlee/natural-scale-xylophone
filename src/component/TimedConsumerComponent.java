package component;

public abstract class TimedConsumerComponent extends TickingComponent {
    private final InputPort<Pulse> timeInputPort;

    public TimedConsumerComponent(BoundedBuffer<Pulse> timeBuffer){
        timeInputPort = new InputPort<>(timeBuffer);

        start();
    }

    @Override
    protected void tick() {
        try {
            timeInputPort.consume();
            timedTick();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected abstract void timedTick();
}