package notes.state;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class VolumeState {

    public final Map<Frequency, Double> volumes;

    VolumeState(Map<Frequency, Double> volumes) {
        this.volumes = volumes;
    }

}
