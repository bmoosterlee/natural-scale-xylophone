package mixer.state;

import frequency.Frequency;

import java.util.Map;

public class AmplitudeState {

    public final Map<Frequency, Double> amplitudes;

    public AmplitudeState(Map<Frequency, Double> amplitudes) {
        this.amplitudes = amplitudes;
    }
}
