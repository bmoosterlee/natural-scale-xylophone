package mixer.state;

import component.buffer.MethodPipeComponent;
import component.buffer.SimpleBuffer;
import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeAmplitudeToVolumeFilter extends MethodPipeComponent<VolumeAmplitudeState, VolumeState> {

    public VolumeAmplitudeToVolumeFilter(SimpleBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer, SimpleBuffer<VolumeState> volumeStateBuffer) {
        super(volumeAmplitudeStateBuffer, volumeStateBuffer, VolumeAmplitudeToVolumeFilter::filter);
    }

    public static VolumeState filter(VolumeAmplitudeState volumeAmplitudeState) {
        Map<Frequency, Double> volumeMap = new HashMap<>();

        for(Frequency frequency : volumeAmplitudeState.volumeAmplitudes.keySet()){
            volumeMap.put(frequency, volumeAmplitudeState.volumeAmplitudes.get(frequency).volume);
        }

        return new VolumeState(volumeMap);
    }
}
