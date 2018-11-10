package mixer.state;

import component.Tickable;
import frequency.Frequency;
import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;

import java.util.HashMap;
import java.util.Map;

public class VolumeAmplitudeToVolumeFilter extends Tickable {

    private final InputPort<VolumeAmplitudeState> volumeAmplitudeStateInput;
    private final OutputPort<VolumeState> volumeStateOutput;

    public VolumeAmplitudeToVolumeFilter(BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer, BoundedBuffer<VolumeState> volumeStateBuffer) {
        volumeAmplitudeStateInput = new InputPort<>(volumeAmplitudeStateBuffer);
        volumeStateOutput = new OutputPort<>(volumeStateBuffer);

        start();
    }

    protected void tick() {
        try {
            VolumeAmplitudeState volumeAmplitudeState = volumeAmplitudeStateInput.consume();

            VolumeState result = filter(volumeAmplitudeState);

            volumeStateOutput.produce(result);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static VolumeState filter(VolumeAmplitudeState volumeAmplitudeState) {
        Map<Frequency, Double> volumeMap = new HashMap<>();

        for(Frequency frequency : volumeAmplitudeState.volumeAmplitudes.keySet()){
            volumeMap.put(frequency, volumeAmplitudeState.volumeAmplitudes.get(frequency).volume);
        }

        return new VolumeState(volumeMap);
    }
}
