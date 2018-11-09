package sound;

import frequency.Frequency;
import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;
import mixer.state.VolumeAmplitude;
import mixer.state.VolumeAmplitudeState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.Map;
import java.util.Set;

public class VolumeAmplitudeStateToSignal implements Runnable{

    private final InputPort<VolumeAmplitudeState> volumeAmplitudeStateInput;
    private final OutputPort<Double> amplitudeOutput;

    public VolumeAmplitudeStateToSignal(BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateInputBuffer, BoundedBuffer<Double> amplitudeOutputBuffer) {

        volumeAmplitudeStateInput = new InputPort<>(volumeAmplitudeStateInputBuffer);
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
            VolumeAmplitudeState volumeAmplitudeState = volumeAmplitudeStateInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("Tick calculateAmplitudes");
            double amplitude = calculateAmplitude(volumeAmplitudeState.volumeAmplitudes.keySet(),
                                                  volumeAmplitudeState.volumeAmplitudes);
            PerformanceTracker.stopTracking(timeKeeper);

            if(amplitude==0.){
                return;
            }

            amplitudeOutput.produce(amplitude);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double calculateAmplitude(Set<Frequency> liveFrequencies, Map<Frequency, VolumeAmplitude> volumeAmplitudeMap) {
        double amplitudeSum = 0;

        for (Frequency frequency : liveFrequencies) {
            VolumeAmplitude volumeAmplitude = volumeAmplitudeMap.get(frequency);

            amplitudeSum += volumeAmplitude.getValue();
        }
        return amplitudeSum;
    }
}