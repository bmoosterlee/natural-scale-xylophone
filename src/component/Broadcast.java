package component;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

public class Broadcast<T> extends Tickable{

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

    public static <T> Collection<BoundedBuffer<T>> broadcast(BoundedBuffer<T> inputBuffer, int size){
        Collection<BoundedBuffer<T>> results = new LinkedList<>();
        for(int i = 0; i<size; i++){
            results.add(new BoundedBuffer<>(1, "broadcast"));
        }
        new Broadcast<>(inputBuffer, results);
        return results;
    }

}
