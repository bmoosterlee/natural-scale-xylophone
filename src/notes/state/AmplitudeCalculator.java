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
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick getLiveNotes");
        NoteSnapshot noteSnapshot = noteManager.getSnapshot(sampleCount);
        FrequencyState frequencyState = noteSnapshot.frequencyState;
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick calculateAmplitudes");
        double amplitude = calculateAmplitude(sampleCount,
                                              frequencyState.getFrequencies(),
                                              frequencyState,
                                              noteSnapshot.waveState);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("notes.NoteEnvironment tick writeToBuffer");
        soundEnvironment.writeToBuffer(amplitude);
        PerformanceTracker.stopTracking(timeKeeper);
    }

    public double calculateAmplitude(long sampleCount, Set<Frequency> liveFrequencies, FrequencyState frequencyState, WaveState waveState) {
        double amplitudeSum = 0;
        for (Frequency frequency : liveFrequencies) {
            Double volume = frequencyState.getVolume(frequency, sampleCount);
            double amplitude = 0.;
            try {
                amplitude = waveState.getAmplitude(frequency, sampleCount);
            } catch (NullPointerException ignored) {

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