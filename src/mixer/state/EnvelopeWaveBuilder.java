package mixer.state;

import frequency.Frequency;
import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.SimpleDeterministicEnvelope;
import mixer.envelope.functions.DeterministicFunction;
import mixer.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import time.TimeInSeconds;

import java.util.Collection;

public class EnvelopeWaveBuilder implements Runnable {
    private final SampleRate sampleRate;
    private final DeterministicFunction envelopeFunction;

    private final InputPort<TimestampedFrequencies> input;
    private final OutputPort<TimestampedNewNotesWithEnvelope> output;

    public EnvelopeWaveBuilder(BoundedBuffer<TimestampedFrequencies> inputBuffer, BoundedBuffer<TimestampedNewNotesWithEnvelope> outputBuffer, SampleRate sampleRate) {
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
            DeterministicEnvelope envelope = new SimpleDeterministicEnvelope(sampleCount, sampleRate, envelopeFunction);
            Collection<Frequency> newNotes = timestampedNewNotes.getFrequencies();
            TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope = new TimestampedNewNotesWithEnvelope(sampleCount, envelope, newNotes);

            output.produce(timestampedNewNotesWithEnvelope);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}