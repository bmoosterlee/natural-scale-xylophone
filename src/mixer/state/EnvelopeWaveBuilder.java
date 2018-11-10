package mixer.state;

import component.*;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.SimpleDeterministicEnvelope;
import mixer.envelope.functions.DeterministicFunction;
import mixer.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import time.TimeInSeconds;

import java.util.Collection;

public class EnvelopeWaveBuilder extends Tickable {
    private CallableWithArguments<TimestampedFrequencies, TimestampedNewNotesWithEnvelope> builder;

    private final InputPort<TimestampedFrequencies> input;
    private final OutputPort<TimestampedNewNotesWithEnvelope> output;

    public EnvelopeWaveBuilder(BoundedBuffer<TimestampedFrequencies> inputBuffer, BoundedBuffer<TimestampedNewNotesWithEnvelope> outputBuffer, SampleRate sampleRate) {
        builder = buildEnvelopeWave(sampleRate);

        input = new InputPort<>(inputBuffer);
        output = new OutputPort<>(outputBuffer);

        start();
    }

    protected void tick() {
        try {
            TimestampedFrequencies timestampedNewNotes = input.consume();
            TimestampedNewNotesWithEnvelope result = builder.call(timestampedNewNotes);
            output.produce(result);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static CallableWithArguments<TimestampedFrequencies, TimestampedNewNotesWithEnvelope> buildEnvelopeWave(SampleRate sampleRate1){
        return new CallableWithArguments<>() {
            private final SampleRate sampleRate = sampleRate1;
            private final DeterministicFunction envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.10, new TimeInSeconds(0.7));

            @Override
            public TimestampedNewNotesWithEnvelope call(TimestampedFrequencies input) {
                long sampleCount = input.getSampleCount();
                DeterministicEnvelope envelope = new SimpleDeterministicEnvelope(sampleCount, sampleRate, envelopeFunction);
                Collection<Frequency> newNotes = input.getFrequencies();
                TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope = new TimestampedNewNotesWithEnvelope(sampleCount, envelope, newNotes);
                return timestampedNewNotesWithEnvelope;
            }
        };
    }

}
