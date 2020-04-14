package sound;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeStateMap {

    public final Map<Frequency, Double> volumes;

    public VolumeStateMap(Map<Frequency, Double> volumes) {
        this.volumes = volumes;
    }

    public VolumeStateMap add(VolumeStateMap other) {
        HashMap<Frequency, Double> newVolumes = new HashMap<>(volumes);

        HashMap<Frequency, Double> existingOtherVolumes = new HashMap<>(other.volumes);
        existingOtherVolumes.keySet().retainAll(newVolumes.keySet());

        HashMap<Frequency, Double> newOtherVolumes = new HashMap<>(other.volumes);
        newOtherVolumes.keySet().removeAll(newVolumes.keySet());

        for(Frequency frequency : existingOtherVolumes.keySet()){
            Double oldVolume = newVolumes.get(frequency);
            Double otherVolume = other.volumes.get(frequency);
            newVolumes.put(frequency, oldVolume + otherVolume);
        }
        newVolumes.putAll(newOtherVolumes);

        return new VolumeStateMap(newVolumes);
    }
}
