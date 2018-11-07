package notes.state;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeAmplitudeState {
    private final Long sampleCount;
    final Map<Frequency, VolumeAmplitude> volumeAmplitudes;

    VolumeAmplitudeState(Long sampleCount, Map<Frequency, VolumeAmplitude> volumeAmplitudes) {
        this.sampleCount = sampleCount;
        this.volumeAmplitudes = volumeAmplitudes;
    }

    public VolumeAmplitudeState add(Frequency frequency, Double volume, Double amplitude) {
        Map<Frequency, VolumeAmplitude> newVolumes = new HashMap<>(volumeAmplitudes);

        try {
            newVolumes.put(frequency, volumeAmplitudes.get(frequency).add(volume));
        }
        catch(NullPointerException e){
            newVolumes.put(frequency, new VolumeAmplitude(volume, amplitude));
        }

        return new VolumeAmplitudeState(sampleCount, newVolumes);
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
}
