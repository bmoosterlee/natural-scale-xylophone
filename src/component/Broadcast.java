package component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Broadcast<T> implements Runnable{

    private final InputPort<T> input;
    private final Collection<OutputPort<T>> outputs;

    public Broadcast(BoundedBuffer<T> inputBuffer, Collection<BoundedBuffer<T>> outputBuffers) {
        input = new InputPort<>(inputBuffer);

        outputs = new HashSet<>();
        for(BoundedBuffer<T> buffer : outputBuffers){
            outputs.add(new OutputPort<>(buffer));
        }

        start();
    }

    private void start() {
        new Thread(this).start();
    }

    public void run() {
        while(true){
            try {
                T item = input.consume();

                for(OutputPort<T> output : outputs){
                    output.produce(item);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
