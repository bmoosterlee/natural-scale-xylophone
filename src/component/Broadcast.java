package component;

import component.buffer.BoundedBuffer;
import component.buffer.InputPort;
import component.buffer.OutputPort;
import component.buffer.SimpleBuffer;
import component.utilities.TickRunner;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

public class Broadcast<T> {

    private final InputPort<T> input;
    private final Collection<OutputPort<T>> outputs;
    private final MyTickRunner tickRunner = new MyTickRunner();

    public Broadcast(BoundedBuffer<T> inputBuffer, Collection<? extends BoundedBuffer<T>> outputBuffers) {
        input = inputBuffer.createInputPort();

        outputs = new HashSet<>();
        for(BoundedBuffer<T> buffer : outputBuffers){
            outputs.add(new OutputPort<>(buffer));
        }

        tickRunner.start();
    }

    private class MyTickRunner extends TickRunner {
        @Override
        protected void tick() {
            Broadcast.this.tick();
        }
    }

    private void tick() {
        try {
            T item = input.consume();
            for(OutputPort<T> output : outputs){
                output.produce(item);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T> Collection<SimpleBuffer<T>> broadcast(BoundedBuffer<T> inputBuffer, int size){
        Collection<SimpleBuffer<T>> results = new LinkedList<>();
        for(int i = 0; i<size; i++){
            results.add(new SimpleBuffer<>(1, "broadcast"));
        }
        new Broadcast<T>(inputBuffer, results);
        return results;
    }
}
