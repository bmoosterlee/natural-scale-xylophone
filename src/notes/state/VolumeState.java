package notes.state;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeState {

    public final Map<Frequency, Double> volumes;

    VolumeState(Map<Frequency, Double> volumes) {
        this.volumes = volumes;
    }

    public VolumeState add(Frequency frequency, double volume) {
        Map<Frequency, Double> newVolumes = new HashMap<>(volumes);

        try {
            newVolumes.put(frequency, volumes.get(frequency) + volume);
        }
        catch(NullPointerException e){
            newVolumes.put(frequency, volume);
        }

        return new VolumeState(newVolumes);
    }
}
