package notes.state;

import main.PerformanceTracker;
import main.TimeKeeper;
import notes.Frequency;

import java.util.Set;

public class AmplitudeCalculator implements Observer<Long> {
    private final SoundEnvironment soundEnvironment;
    private final NoteManager noteManager;

    public AmplitudeCalculator(SoundEnvironment soundEnvironment, NoteManager noteManager) {
        this.soundEnvironment = soundEnvironment;
        this.noteManager = noteManager;
    }

    public void tick(long sampleCount) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("Tick getLiveNotes");
        FrequencyState frequencyState = noteManager.getFrequencyState(sampleCount);
        WaveState waveState = noteManager.getWaveState(sampleCount);
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

    @Override
    public void notify(Long event) {
        tick(event);
    }
}