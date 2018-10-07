package wave.state;

import frequency.Frequency;

import java.util.Set;

public interface WaveState {
    WaveState update(Set<Frequency> frequencies);

    WaveState update(long sampleCount);

    double getAmplitude(Frequency frequency, long sampleCount);
}
