package mixer.state;

import component.buffers.BoundedBuffer;
import component.buffers.SimpleBuffer;
import component.utilities.TickablePipeComponent;
import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeAmplitudeToVolumeFilter extends TickablePipeComponent<VolumeAmplitudeState, VolumeState> {

    public VolumeAmplitudeToVolumeFilter(BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer, SimpleBuffer<VolumeState> volumeStateBuffer) {
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
