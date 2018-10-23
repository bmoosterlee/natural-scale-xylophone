package notes.state;

import frequency.Frequency;
import wave.Wave;

import java.util.HashMap;
import java.util.Map;

public class VolumeAmplitudeState {

    public final Map<Frequency, VolumeAmplitude> volumeAmplitudes;

    VolumeAmplitudeState(Map<Frequency, VolumeAmplitude> volumeAmplitudes) {
        this.volumeAmplitudes = volumeAmplitudes;
    }

    public VolumeAmplitudeState add(Frequency frequency, Double volume, Wave wave, long sampleCount) {
        Map<Frequency, VolumeAmplitude> newVolumes = new HashMap<>(volumeAmplitudes);

        try {
            newVolumes.put(frequency, volumeAmplitudes.get(frequency).add(volume));
        }
        catch(NullPointerException e){
            newVolumes.put(frequency, new VolumeAmplitude(volume, wave.getAmplitude(sampleCount)));
        }

        return new VolumeAmplitudeState(newVolumes);
    }
    
}
