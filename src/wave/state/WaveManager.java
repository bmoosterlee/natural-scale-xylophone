package wave.state;

import frequency.state.FrequencyManager;
import frequency.state.FrequencyState;
import sound.SampleRate;

public class WaveManager {
    private final FrequencyManager frequencyManager;

    WaveState waveState;
    long updatedToSample = -1;

    public WaveManager(FrequencyManager frequencyManager, SampleRate sampleRate) {
        this.frequencyManager = frequencyManager;

        waveState = new WaveState(sampleRate);
    }

    public WaveState getWaveState(long sampleCount) {
        synchronized (this) {
            if(sampleCount>updatedToSample) {
                FrequencyState frequencyState = frequencyManager.getFrequencyState(sampleCount);
                waveState = waveState.update(frequencyState.getFrequencies(), frequencyState.buckets);
                waveState = waveState.update(sampleCount);
                updatedToSample = sampleCount;
            }
            return waveState;
        }
    }
}