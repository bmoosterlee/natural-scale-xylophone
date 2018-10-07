package notes.state;

import frequency.state.FrequencyManager;
import frequency.state.FrequencyState;
import time.PerformanceTracker;
import time.TimeKeeper;
import frequency.Frequency;
import sound.SoundEnvironment;
import wave.state.WaveManager;
import wave.state.WaveState;

import java.util.Set;

public class AmplitudeCalculator {
    private final SoundEnvironment soundEnvironment;
    private final FrequencyManager frequencyManager;
    private final WaveManager waveManager;

    public AmplitudeCalculator(SoundEnvironment soundEnvironment, FrequencyManager frequencyManager, WaveManager waveManager) {
        this.soundEnvironment = soundEnvironment;
        this.frequencyManager = frequencyManager;
        this.waveManager = waveManager;
    }

    public void tick(long sampleCount) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("Tick getFrequencyState");
        FrequencyState frequencyState = frequencyManager.getFrequencyState(sampleCount);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("Tick getWaveState");
        WaveState waveState = waveManager.getWaveState(sampleCount);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("Tick calculateAmplitudes");
        double amplitude = calculateAmplitude(sampleCount,
                                              frequencyState.getFrequencies(),
                                              frequencyState,
                                              waveState);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("Tick writeToBuffer");
        soundEnvironment.writeToBuffer(amplitude);
        PerformanceTracker.stopTracking(timeKeeper);
    }

    public double calculateAmplitude(long sampleCount, Set<Frequency> liveFrequencies, FrequencyState frequencyState, WaveState waveState) {
        double amplitudeSum = 0;
        for (Frequency frequency : liveFrequencies) {
            Double volume = frequencyState.getVolume(frequency, sampleCount);
            Double amplitude;
            try {
                amplitude = waveState.getAmplitude(frequency, sampleCount);
            } catch (NullPointerException e) {
                amplitude = 0.;
            }

            amplitudeSum += volume * amplitude;
        }
        return amplitudeSum;
    }

}