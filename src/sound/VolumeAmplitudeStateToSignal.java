package sound;

import component.buffer.*;
import component.utilities.RunningPipeComponent;
import frequency.Frequency;
import mixer.state.VolumeAmplitude;
import mixer.state.VolumeAmplitudeState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.Map;
import java.util.Set;

public class VolumeAmplitudeStateToSignal extends RunningPipeComponent<VolumeAmplitudeState, Double> {

    public VolumeAmplitudeStateToSignal(BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateInputBuffer, SimpleBuffer<Double> amplitudeOutputBuffer) {
        super(volumeAmplitudeStateInputBuffer, amplitudeOutputBuffer, input -> calculateAmplitude(input.volumeAmplitudes));
    }


    public static double calculateAmplitude(Map<Frequency, VolumeAmplitude> volumeAmplitudeMap) {
        double amplitudeSum = 0;

        for (Frequency frequency : volumeAmplitudeMap.keySet()) {
            VolumeAmplitude volumeAmplitude = volumeAmplitudeMap.get(frequency);

            amplitudeSum += volumeAmplitude.getValue();
        }
        return amplitudeSum;
    }
}