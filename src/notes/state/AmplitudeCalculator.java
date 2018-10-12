package notes.state;

import frequency.Frequency;
import frequency.state.FrequencyManager;
import frequency.state.FrequencyState;
import notes.envelope.EnvelopeManager;
import notes.envelope.EnvelopeState;
import sound.SoundEnvironment;
import time.PerformanceTracker;
import time.TimeKeeper;
import wave.state.WaveManager;
import wave.state.WaveState;

import java.util.Set;

public class AmplitudeCalculator {
    private final SoundEnvironment soundEnvironment;
    private final FrequencyManager frequencyManager;
    private final EnvelopeManager envelopeManager;
    private final WaveManager waveManager;

    public AmplitudeCalculator(SoundEnvironment soundEnvironment, FrequencyManager frequencyManager, EnvelopeManager envelopeManager, WaveManager waveManager) {
        this.soundEnvironment = soundEnvironment;
        this.frequencyManager = frequencyManager;
        this.envelopeManager = envelopeManager;
        this.waveManager = waveManager;
    }

    public void tick(long sampleCount) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("Tick getFrequencyState");
        FrequencyState frequencyState = frequencyManager.getFrequencyState(sampleCount);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("Tick getEnvelopeState");
        EnvelopeState envelopeState = envelopeManager.getEnvelopeState(sampleCount);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("Tick getWaveState");
        WaveState waveState = waveManager.getWaveState(sampleCount);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("Tick calculateAmplitudes");
        double amplitude = calculateAmplitude(sampleCount,
                                              frequencyState.getFrequencies(),
                                              frequencyState,
                                              envelopeState,
                                              waveState);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("Tick writeToBuffer");
        soundEnvironment.writeToBuffer(amplitude);
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private double calculateAmplitude(long sampleCount, Set<Frequency> liveFrequencies, FrequencyState frequencyState, EnvelopeState envelopeState, WaveState waveState) {
        double amplitudeSum = 0;
        for (Frequency frequency : liveFrequencies) {
            Double volume = frequencyState.getVolume(frequency, envelopeState, sampleCount);
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