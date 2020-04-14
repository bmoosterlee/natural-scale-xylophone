package sound;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class AmplitudeStateMap {

    public final Map<Frequency, Double> amplitudes;

    public AmplitudeStateMap(Map<Frequency, Double> amplitudes) {
        this.amplitudes = amplitudes;
    }

    public AmplitudeStateMap add(AmplitudeStateMap other) {
        HashMap<Frequency, Double> newAmplitudes = new HashMap<>(amplitudes);
        newAmplitudes.putAll(other.amplitudes);
        return new AmplitudeStateMap(newAmplitudes);
    }
}
