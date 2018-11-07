package notes.state;

import frequency.Frequency;
import notes.envelope.Envelope;
import wave.Wave;

import java.util.Collection;
import java.util.Map;

public class EnvelopeWaveSlice {
    private final long sampleCount;
    private final Map<Frequency, Collection<Envelope>> envelopesPerFrequency;
    private final Map<Frequency, Wave> wavesPerFrequency;

    public EnvelopeWaveSlice(long sampleCount, Map<Frequency, Collection<Envelope>> envelopesPerFrequency, Map<Frequency, Wave> wavesPerFrequency) {
        this.sampleCount = sampleCount;
        this.envelopesPerFrequency = envelopesPerFrequency;
        this.wavesPerFrequency = wavesPerFrequency;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public Map<Frequency, Collection<Envelope>> getEnvelopesPerFrequency() {
        return envelopesPerFrequency;
    }

    public Map<Frequency, Wave> getWavesPerFrequency() {
        return wavesPerFrequency;
    }
}
