package spectrum.buckets;

import component.buffer.SimpleBuffer;
import component.buffer.BoundedBuffer;
import component.buffer.InputPort;
import component.buffer.OutputPort;

import java.util.HashSet;
import java.util.Set;

public class Adder implements Runnable {

    private final Set<InputPort<Buckets>> inputs;
    private final OutputPort<Buckets> output;

    public Adder(Set<BoundedBuffer<Buckets>> inputBuffers, SimpleBuffer<Buckets> outputBuffer) {
        inputs = new HashSet<>();
        for(BoundedBuffer<Buckets> inputBuffer : inputBuffers){
            inputs.add(inputBuffer.createInputPort());
        }

        output = new OutputPort<>(outputBuffer);

        start();
    }

    private void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    private void tick() {
        try {
            Buckets accumulator = null;

            for(InputPort<Buckets> input : inputs){
                Buckets buckets = input.consume();
                try{
                    accumulator = accumulator.add(buckets);
                }
                catch(NullPointerException e){
                    accumulator = buckets;
                }
            }

            output.produce(accumulator);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
