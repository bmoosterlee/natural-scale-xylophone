package main;

public class IntegratedTimedConsumerComponent<T> extends Component{
    private final InputPort<Pulse> timeInputPort;
    private final InputPort<T> inputPort;
    private final OutputPort<T> outputPort;

    public IntegratedTimedConsumerComponent(BoundedBuffer<Pulse> timeBuffer, BoundedBuffer<T> inputBuffer, BoundedBuffer<T> outputBuffer){
        timeInputPort = new InputPort<>(timeBuffer);
        inputPort = new InputPort<>(inputBuffer);
        outputPort = new OutputPort<>(outputBuffer);

        start();
    }

    @Override
    protected void tick() {
        try {
            timeInputPort.consume();
            T result = inputPort.consume();
            outputPort.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
