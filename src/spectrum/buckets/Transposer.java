package spectrum.buckets;

import component.buffer.*;
import component.buffer.RunningPipeComponent;

public class Transposer extends RunningPipeComponent<Buckets, Buckets> {

    public Transposer(SimpleBuffer<Buckets> inputBuffer, SimpleBuffer<Buckets> outputBuffer, int transposition) {
        super(inputBuffer, outputBuffer, input -> input.transpose(transposition));

        start();
    }
}
