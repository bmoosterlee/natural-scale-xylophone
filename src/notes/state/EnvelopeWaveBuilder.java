package notes.state;

import frequency.Frequency;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import notes.envelope.DeterministicEnvelope;
import notes.envelope.SimpleDeterministicEnvelope;
import notes.envelope.functions.DeterministicFunction;
import notes.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import time.TimeInSeconds;
import wave.Wave;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EnvelopeWaveBuilder implements Runnable {
    private final SampleRate sampleRate;
    private final DeterministicFunction envelopeFunction;

    private final InputPort<TimestampedFrequencies> input;
    private final OutputPort<TimestampedEnvelopeWaves> output;

    public EnvelopeWaveBuilder(BoundedBuffer<TimestampedFrequencies> inputBuffer, BoundedBuffer<TimestampedEnvelopeWaves> outputBuffer, SampleRate sampleRate) {
        this.sampleRate = sampleRate;
        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.10, new TimeInSeconds(0.7));

        input = new InputPort<>(inputBuffer);
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
            TimestampedFrequencies timestampedNewNotes = input.consume();

            long sampleCount = timestampedNewNotes.getSampleCount();
            Collection<Frequency> newNotes = timestampedNewNotes.getFrequencies();

            DeterministicEnvelope envelope = new SimpleDeterministicEnvelope(sampleCount, sampleRate, envelopeFunction);
            Map<Frequency, Wave> waves = new HashMap<>();
            for(Frequency frequency : newNotes){
                waves.put(frequency, new Wave(frequency, sampleRate));
            }

            TimestampedEnvelopeWaves timestampedEnvelopeWaves = new TimestampedEnvelopeWaves(sampleCount, envelope, waves);

            output.produce(timestampedEnvelopeWaves);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
