package mixer.state;

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
        for(Frequency frequency : other.amplitudes.keySet()){
            Double oldAmplitude = newAmplitudes.get(frequency);
            Double otherAmplitude = other.amplitudes.get(frequency);
            try {
                newAmplitudes.put(frequency, oldAmplitude + otherAmplitude);
            }
            catch(NullPointerException e){
                newAmplitudes.put(frequency, otherAmplitude);
            }
        }
        return new AmplitudeState(newAmplitudes);
    }
}
