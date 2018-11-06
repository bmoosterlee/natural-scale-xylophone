package notes.state;

import frequency.Frequency;
import notes.envelope.DeterministicEnvelope;
import wave.Wave;

import java.util.Map;

public class TimestampedEnvelopeWaves {
    private final Long sampleCount;
    private final DeterministicEnvelope envelope;
    private final Map<Frequency, Wave> waves;

    public TimestampedEnvelopeWaves(Long sampleCount, DeterministicEnvelope envelope, Map<Frequency, Wave> waves) {
        this.sampleCount = sampleCount;
        this.envelope = envelope;
        this.waves = waves;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public DeterministicEnvelope getEnvelope() {
        return envelope;
    }

    public Map<Frequency, Wave> getWaves() {
        return waves;
    }
}
