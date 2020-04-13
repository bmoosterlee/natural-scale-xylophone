package sound;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Map;

public class AmplitudeState {

    public final Map<Frequency, Double> amplitudes;

    public AmplitudeState(Map<Frequency, Double> amplitudes) {
        this.amplitudes = amplitudes;
    }

    public AmplitudeState add(AmplitudeState other) {
        HashMap<Frequency, Double> newAmplitudes = new HashMap<>(amplitudes);
        newAmplitudes.putAll(other.amplitudes);
        return new AmplitudeState(newAmplitudes);
    }
}
