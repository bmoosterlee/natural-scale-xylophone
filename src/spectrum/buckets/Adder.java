package spectrum.buckets;

import component.buffer.*;

import java.util.HashSet;
import java.util.Set;

public class Adder {
    private final Set<InputPort<Buckets>> inputs;
    private final OutputPort<Buckets> output;

    private final TickRunner tickRunner = new MyTickRunner();

    public Adder(Set<BoundedBuffer<Buckets>> inputBuffers, SimpleBuffer<Buckets> outputBuffer) {
        inputs = new HashSet<>();
        for(BoundedBuffer<Buckets> inputBuffer : inputBuffers){
            inputs.add(inputBuffer.createInputPort());
        }

        output = new OutputPort<>(outputBuffer);

        start();
    }

    private void start() {
        tickRunner.start();
    }

    private class MyTickRunner extends SimpleTickRunner {

        @Override
        protected void tick() {
            Adder.this.tick();
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

    public static SimpleBuffer<Buckets> build(Set<BoundedBuffer<Buckets>> inputBuffers){
        SimpleBuffer<Buckets> outputBuffer = new SimpleBuffer<>(1, "adder - output");
        new Adder(inputBuffers, outputBuffer);
        return outputBuffer;
    }
}
