package mixer.state;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeAmplitudeState {

    public static Map<Frequency, VolumeAmplitude> build(VolumeState volumeState, AmplitudeState amplitudeState){
        Map<Frequency, VolumeAmplitude> volumeAmplitudes = new HashMap<>();
        for(Frequency frequency : volumeState.volumes.keySet()){
            volumeAmplitudes.put(frequency,
                    new VolumeAmplitude(
                            volumeState.volumes.get(frequency),
                            amplitudeState.amplitudes.get(frequency)));
        }
        return volumeAmplitudes;
    }

    public static Double toDouble(Map<Frequency, VolumeAmplitude> volumeAmplitudes){
        return volumeAmplitudes.values().stream()
                .map(VolumeAmplitude::getValue)
                .reduce(0., Double::sum);
    }
}
