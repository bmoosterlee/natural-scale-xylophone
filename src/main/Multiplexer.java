package main;

import java.util.HashSet;
import java.util.Set;

public class Multiplexer<T> implements Runnable{

    private InputPort<T> input;
    private Set<OutputPort<T>> outputs;

    Multiplexer(BoundedBuffer<T> inputBuffer, Set<BoundedBuffer<T>> outputBuffers) {
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
