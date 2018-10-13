package notes.state;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeState {

    public final Long sampleCount;
    public final Map<Frequency, Double> volumes;

    VolumeState(Long sampleCount, Map<Frequency, Double> volumes) {
        this.sampleCount = sampleCount;
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

        return new VolumeState(sampleCount, newVolumes);
    }
}
