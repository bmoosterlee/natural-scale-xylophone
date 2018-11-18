package component;

import component.buffer.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

public class Broadcast<T> {

    private final InputPort<T> input;
    private final Collection<OutputPort<T>> outputs;

    public Broadcast(SimpleBuffer<T> inputBuffer, Collection<SimpleBuffer<T>> outputBuffers) {
        input = inputBuffer.createInputPort();

        outputs = new HashSet<>();
        for(SimpleBuffer<T> buffer : outputBuffers){
            outputs.add(new OutputPort<>(buffer));
        }

        new TickRunnerSpawner(inputBuffer) {
            @Override
            protected void tick() {
                Broadcast.this.tick();
            }
        }.start();
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

    public static <T> Collection<SimpleBuffer<T>> broadcast(SimpleBuffer<T> inputBuffer, int size){
        return broadcast(inputBuffer, size, "unnamed broadcast");
    }

    public static <T> Collection<SimpleBuffer<T>> broadcast(SimpleBuffer<T> inputBuffer, int size, String name) {
        Collection<SimpleBuffer<T>> outputBuffers = new LinkedList<>();
        for(int i = 0; i<size; i++){
            outputBuffers.add(new SimpleBuffer<>(1, name));
        }
        new Broadcast<>(inputBuffer, outputBuffers);
        return outputBuffers;
    }
}
