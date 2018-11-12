package spectrum.buckets;

import component.buffer.*;
import component.utilities.RunningPipeComponent;
import component.utilities.TickRunner;

public class Multiplier extends RunningPipeComponent<Buckets, Buckets> {

    public Multiplier(BoundedBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, double multiplier) {
        super(inputBuffer, outputBuffer, input -> input.multiply(multiplier));
    }
}
