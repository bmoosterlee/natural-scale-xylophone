package notes.state;

import frequency.Frequency;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import sound.SampleRate;
import time.PerformanceTracker;
import time.TimeKeeper;
import wave.Wave;

import java.util.Map;
import java.util.Set;

public class AmplitudeCalculator implements Runnable{

    private SampleRate sampleRate;

    private InputPort<VolumeState> volumeStateInput;
    private OutputPort<Double> amplitudeOutput;

    public AmplitudeCalculator(BoundedBuffer<VolumeState> volumeStateInputBuffer, BoundedBuffer<Double> amplitudeOutputBuffer, SampleRate sampleRate) {
        this.sampleRate = sampleRate;

        volumeStateInput = new InputPort<>(volumeStateInputBuffer);
        amplitudeOutput = new OutputPort<>(amplitudeOutputBuffer);

        start();
    }

    private void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    public void tick() {
        try {
            TimeKeeper timeKeeper = PerformanceTracker.startTracking("Tick getWaveState");
            VolumeState volumeState = volumeStateInput.consume();
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("Tick calculateAmplitudes");
            double amplitude = calculateAmplitude(  volumeState.sampleCount,
                                                    volumeState.volumes.keySet(),
                                                    volumeState.volumes);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("Tick writeToBuffer");
            amplitudeOutput.produce(amplitude);
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double calculateAmplitude(long sampleCount, Set<Frequency> liveFrequencies, Map<Frequency, Double> volumes) {
        double amplitudeSum = 0;

        for (Frequency frequency : liveFrequencies) {
            Double volume = volumes.get(frequency);
            Double amplitude = Wave.getAmplitude(sampleRate, frequency, sampleCount);

            amplitudeSum += volume * amplitude;
        }
        return amplitudeSum;
    }
}