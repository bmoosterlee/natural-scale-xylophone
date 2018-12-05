package mixer.state;

import frequency.Frequency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AmplitudeState {

    public final Map<Frequency, Double> amplitudes;

    public AmplitudeState(Map<Frequency, Double> amplitudes) {
        this.amplitudes = amplitudes;
    }

    public AmplitudeState add(AmplitudeState other) {
        HashMap<Frequency, Double> newAmplitudes = new HashMap<>(amplitudes);

        HashMap<Frequency, Double> newOtherAmplitudes = new HashMap<>(other.amplitudes);
        newOtherAmplitudes.keySet().removeAll(newAmplitudes.keySet());
        newAmplitudes.putAll(newOtherAmplitudes);

        return new AmplitudeState(newAmplitudes);
    }
}
