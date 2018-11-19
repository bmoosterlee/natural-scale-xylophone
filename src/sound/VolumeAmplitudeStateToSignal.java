package sound;

import component.buffer.*;
import frequency.Frequency;
import mixer.state.VolumeAmplitude;
import mixer.state.VolumeAmplitudeState;

import java.util.Map;

public class VolumeAmplitudeStateToSignal extends MethodPipeComponent<VolumeAmplitudeState, Double> {

    public VolumeAmplitudeStateToSignal(SimpleBuffer<VolumeAmplitudeState> volumeAmplitudeStateInputBuffer, SimpleBuffer<Double> amplitudeOutputBuffer) {
        super(volumeAmplitudeStateInputBuffer, amplitudeOutputBuffer, build());
    }

    public static CallableWithArguments<VolumeAmplitudeState, Double> build() {
        return VolumeAmplitudeStateToSignal::calculateAmplitude;
    }

    public static double calculateAmplitude(VolumeAmplitudeState volumeAmplitudeState) {
        Map<Frequency, VolumeAmplitude> volumeAmplitudeMap = volumeAmplitudeState.volumeAmplitudes;

        double amplitudeSum = 0;

        for (Frequency frequency : volumeAmplitudeMap.keySet()) {
            VolumeAmplitude volumeAmplitude = volumeAmplitudeMap.get(frequency);

            amplitudeSum += volumeAmplitude.getValue();
        }
        return amplitudeSum;
    }

}