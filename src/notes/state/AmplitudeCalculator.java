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

    private final SampleRate sampleRate;

    private final InputPort<Long> sampleCountInput;
    private final InputPort<VolumeState> volumeStateInput;
    private final OutputPort<Double> amplitudeOutput;

    public AmplitudeCalculator(BoundedBuffer<Long> sampleCountInputBuffer, BoundedBuffer<VolumeState> volumeStateInputBuffer, BoundedBuffer<Double> amplitudeOutputBuffer, SampleRate sampleRate) {
        this.sampleRate = sampleRate;

        sampleCountInput = new InputPort<>(sampleCountInputBuffer);
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

    private void tick() {
        try {
            Long sampleCount = sampleCountInput.consume();
            VolumeState volumeState = volumeStateInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("Tick calculateAmplitudes");
            double amplitude = calculateAmplitude(  sampleCount,
                                                    volumeState.volumes.keySet(),
                                                    volumeState.volumes);
            PerformanceTracker.stopTracking(timeKeeper);

            amplitudeOutput.produce(amplitude);

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