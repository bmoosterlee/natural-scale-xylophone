package spectrum.buckets;

import component.buffer.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Adder extends AbstractComponent<Buckets, Buckets> {
    private final Set<InputPort<Buckets>> inputs;
    private final OutputPort<Buckets> output;

    public Adder(Set<BoundedBuffer<Buckets>> inputBuffers, SimpleBuffer<Buckets> outputBuffer) {
        inputs = new HashSet<>();
        for(BoundedBuffer<Buckets> inputBuffer : inputBuffers){
            inputs.add(inputBuffer.createInputPort());
        }

        output = new OutputPort<>(outputBuffer);

        new SimpleTickRunner(this).start();
    }

    @Override
    protected Collection<BoundedBuffer<Buckets>> getInputBuffers() {
        Collection<BoundedBuffer<Buckets>> inputBuffers = new HashSet<>();
        for(InputPort<Buckets> outputPort : inputs){
            inputBuffers.add(outputPort.getBuffer());
        }
        return inputBuffers;
    }

    @Override
    protected Collection<BoundedBuffer<Buckets>> getOutputBuffers() {
        return Collections.singleton(output.getBuffer());
    }

    protected void tick() {
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
