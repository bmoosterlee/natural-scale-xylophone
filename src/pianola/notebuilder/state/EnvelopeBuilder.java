package pianola.notebuilder.state;

import component.buffer.*;
import frequency.Frequency;
import pianola.notebuilder.envelope.DeterministicEnvelope;
import pianola.notebuilder.envelope.SimpleDeterministicEnvelope;
import pianola.notebuilder.envelope.functions.DeterministicFunction;
import pianola.notebuilder.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import time.TimeInSeconds;

import java.util.Collection;

public class EnvelopeBuilder {

    public static PipeCallable<TimestampedFrequencies, TimestampedNewNotesWithEnvelope> buildEnvelope(SampleRate sampleRate){
        return new PipeCallable<>() {
            private final DeterministicFunction envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.10, new TimeInSeconds(0.7));

            @Override
            public TimestampedNewNotesWithEnvelope call(TimestampedFrequencies input) {
                long sampleCount = input.getSampleCount();
                DeterministicEnvelope envelope = new SimpleDeterministicEnvelope(sampleCount, sampleRate, envelopeFunction);
                Collection<Frequency> newNotes = input.getFrequencies();
                return new TimestampedNewNotesWithEnvelope(sampleCount, envelope, newNotes);
            }
        };
    }

}
