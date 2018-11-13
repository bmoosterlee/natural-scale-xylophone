package spectrum.buckets;

import component.buffer.*;
import component.buffer.RunningPipeComponent;

public class Multiplier extends RunningPipeComponent<Buckets, Buckets> {

    public Multiplier(BoundedBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, double multiplier) {
        super(inputBuffer, outputBuffer, input -> input.multiply(multiplier));
    }
}
