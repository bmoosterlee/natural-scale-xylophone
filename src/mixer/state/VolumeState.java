package mixer.state;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeState {

    public final Map<Frequency, Double> volumes;

    public VolumeState(Map<Frequency, Double> volumes) {
        this.volumes = volumes;
    }

    public VolumeState add(VolumeState other) {
        HashMap<Frequency, Double> newVolumes = new HashMap<>(volumes);
        for(Frequency frequency : other.volumes.keySet()){
            Double oldVolume = newVolumes.get(frequency);
            Double otherVolume = other.volumes.get(frequency);
            try {
                newVolumes.put(frequency, oldVolume + otherVolume);
            }
            catch(NullPointerException e){
                newVolumes.put(frequency, otherVolume);
            }
        }
        return new VolumeState(newVolumes);
    }
}
