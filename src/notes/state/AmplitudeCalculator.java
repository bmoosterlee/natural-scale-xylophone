package notes.state;

import frequency.Frequency;
import frequency.state.FrequencyManager;
import frequency.state.FrequencyState;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import notes.envelope.EnvelopeManager;
import notes.envelope.EnvelopeState;
import time.PerformanceTracker;
import time.TimeKeeper;
import wave.state.WaveManager;
import wave.state.WaveState;

import java.util.Set;

public class AmplitudeCalculator {
    private final FrequencyManager frequencyManager;
    private final EnvelopeManager envelopeManager;

    private OutputPort<Long> sampleCountOutput;
    private InputPort<WaveState> waveStateInput;

    private OutputPort<Double> sampleAmplitude;

    public AmplitudeCalculator(FrequencyManager frequencyManager, EnvelopeManager envelopeManager, BoundedBuffer<Double> amplitudeOutputBuffer, BoundedBuffer<Long> sampleCountOutputBuffer, BoundedBuffer<WaveState> waveStateInputBuffer) {
        this.frequencyManager = frequencyManager;
        this.envelopeManager = envelopeManager;

        sampleCountOutput = new OutputPort<>(sampleCountOutputBuffer);
        waveStateInput = new InputPort<>(waveStateInputBuffer);

        sampleAmplitude = new OutputPort<>(amplitudeOutputBuffer);
    }

    public void tick(long sampleCount) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("Tick getFrequencyState");
        FrequencyState frequencyState = frequencyManager.getFrequencyState(sampleCount);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("Tick getEnvelopeState");
        EnvelopeState envelopeState = envelopeManager.getEnvelopeState(sampleCount);
        PerformanceTracker.stopTracking(timeKeeper);

        try {
            timeKeeper = PerformanceTracker.startTracking("Tick getWaveState");
            sampleCountOutput.produce(sampleCount);
            WaveState waveState = waveStateInput.consume();
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("Tick calculateAmplitudes");
            double amplitude = calculateAmplitude(sampleCount,
                                                  frequencyState.getFrequencies(),
                                                  frequencyState,
                                                  envelopeState,
                                                  waveState);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("Tick writeToBuffer");
            try {
                sampleAmplitude.produce(amplitude);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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