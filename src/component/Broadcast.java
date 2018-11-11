package component;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

public class Broadcast<T> extends Tickable{

    private final InputPort<T> input;
    private final Collection<OutputPort<T>> outputs;

    public Broadcast(BufferInterface<T> inputBuffer, Collection<BufferInterface<T>> outputBuffers) {
        input = new InputPort<>(inputBuffer);

        outputs = new HashSet<>();
        for(BufferInterface<T> buffer : outputBuffers){
            outputs.add(new OutputPort<>(buffer));
        }

        start();
    }

    @Override
    protected void tick() {
        try {
            T item = input.consume();
            for(OutputPort<T> output : outputs){
                output.produce(item);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected static <T> Collection<BufferInterface<T>> broadcast(BufferInterface<T> inputBuffer, int size){
        Collection<BufferInterface<T>> results = new LinkedList<>();
        for(int i = 0; i<size; i++){
            results.add(new BoundedBuffer<>(1, "broadcast"));
        }
        new Broadcast<>(inputBuffer, results);
        return results;
    }

}
