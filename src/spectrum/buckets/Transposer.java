package spectrum.buckets;

import component.buffer.*;
import component.utilities.RunningPipeComponent;
import component.utilities.TickRunner;

public class Transposer extends RunningPipeComponent<Buckets, Buckets> {

    public Transposer(BoundedBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, int transposition) {
        super(inputBuffer, outputBuffer, input -> input.transpose(transposition));

        start();
    }
}
