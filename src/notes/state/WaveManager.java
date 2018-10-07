package notes.state;

import sound.SampleRate;
import wave.WaveState;

public class WaveManager {
    private final FrequencyManager frequencyManager;

    WaveState waveState;
    long updatedToSample = -1;

    public WaveManager(FrequencyManager frequencyManager, SampleRate sampleRate) {
        this.frequencyManager = frequencyManager;

        waveState = new WaveState(sampleRate);
    }

    public WaveState getWaveState(long sampleCount) {
        synchronized (frequencyManager) {
            if(sampleCount>updatedToSample) {
                waveState = waveState.update(frequencyManager.getFrequencyState(sampleCount).getFrequencies());
                waveState = waveState.update(sampleCount);
                updatedToSample = sampleCount;
            }
            return waveState;
        }
    }
}