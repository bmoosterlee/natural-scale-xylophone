package pianola.notebuilder.state;

import frequency.Frequency;

import java.util.Collection;

public class TimestampedFrequencies {

    private final long sampleCount;
    private final Collection<Frequency> frequencies;

    public TimestampedFrequencies(long sampleCount, Collection<Frequency> frequencies) {
        this.sampleCount = sampleCount;
        this.frequencies = frequencies;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public Collection<Frequency> getFrequencies() {
        return frequencies;
    }
}
