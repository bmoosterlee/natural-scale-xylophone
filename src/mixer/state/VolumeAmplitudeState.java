package mixer.state;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeAmplitudeState {
    public final Map<Frequency, VolumeAmplitude> volumeAmplitudes;

    public VolumeAmplitudeState(Map<Frequency, VolumeAmplitude> volumeAmplitudes) {
        this.volumeAmplitudes = volumeAmplitudes;
    }

    public VolumeAmplitudeState(VolumeState volumeState, AmplitudeState amplitudeState) {
        volumeAmplitudes = new HashMap<>();
        try {
            for(Frequency frequency : volumeState.volumes.keySet()){
                volumeAmplitudes.put(frequency,
                        new VolumeAmplitude(
                                volumeState.volumes.get(frequency),
                                amplitudeState.amplitudes.get(frequency)));
            }
        }
        catch(NullPointerException ignored){
        }
    }

    public VolumeAmplitudeState add(Frequency frequency, Double volume, Double amplitude) {
        Map<Frequency, VolumeAmplitude> newVolumes = new HashMap<>(volumeAmplitudes);

        try {
            newVolumes.put(frequency, volumeAmplitudes.get(frequency).add(volume));
        }
        catch(NullPointerException e){
            newVolumes.put(frequency, new VolumeAmplitude(volume, amplitude));
        }

        return new VolumeAmplitudeState(newVolumes);
    }

    public VolumeAmplitudeState add(Frequency frequency, VolumeAmplitude volumeAmplitude) {
        return add(frequency, volumeAmplitude.volume, volumeAmplitude.amplitude);
    }

    public VolumeAmplitudeState add(Map<Frequency, VolumeAmplitude> volumeAmplitudes){
        VolumeAmplitudeState newVolumeAmplitudeState = this;

        for(Frequency frequency : volumeAmplitudes.keySet()){
            newVolumeAmplitudeState = newVolumeAmplitudeState.add(frequency, volumeAmplitudes.get(frequency));
        }

        return newVolumeAmplitudeState;
    }

    public VolumeState toVolumeState() {
        Map<Frequency, Double> volumeMap = new HashMap<>();

        for(Frequency frequency : volumeAmplitudes.keySet()){
            volumeMap.put(frequency, volumeAmplitudes.get(frequency).volume);
        }

        return new VolumeState(volumeMap);
    }

    public AmplitudeState toAmplitudeState() {
        Map<Frequency, Double> amplitudeMap = new HashMap<>();

        for(Frequency frequency : volumeAmplitudes.keySet()){
            amplitudeMap.put(frequency, volumeAmplitudes.get(frequency).amplitude);
        }

        return new AmplitudeState(amplitudeMap);
    }

}
