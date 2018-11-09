package mixer.state;

import frequency.Frequency;
import component.BoundedBuffer;
import component.InputPort;
import component.OutputPort;

import java.util.HashMap;
import java.util.Map;

public class VolumeAmplitudeToVolumeFilter implements Runnable{

    private final InputPort<VolumeAmplitudeState> volumeAmplitudeStateInput;
    private final OutputPort<VolumeState> volumeStateOutput;

    public VolumeAmplitudeToVolumeFilter(BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer, BoundedBuffer<VolumeState> volumeStateBuffer) {
        volumeAmplitudeStateInput = new InputPort<>(volumeAmplitudeStateBuffer);
        volumeStateOutput = new OutputPort<>(volumeStateBuffer);

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

            Map<Frequency, Double> volumeMap = new HashMap<>();

            for(Frequency frequency : volumeAmplitudeState.volumeAmplitudes.keySet()){
                volumeMap.put(frequency, volumeAmplitudeState.volumeAmplitudes.get(frequency).volume);
            }

            volumeStateOutput.produce(new VolumeState(volumeMap));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
