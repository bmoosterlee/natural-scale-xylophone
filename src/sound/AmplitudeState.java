package sound;

public class AmplitudeState {

    public final Double[] amplitudes;

    public AmplitudeState(Double[] amplitudes) {
        this.amplitudes = amplitudes;
    }

    public AmplitudeState add(AmplitudeState other) {
        Double[] newAmplitudes = new Double[amplitudes.length];

        for(int i = 0; i<amplitudes.length; i++){
            newAmplitudes[i] = amplitudes[i] + other.amplitudes[i];
        }

        return new AmplitudeState(newAmplitudes);
    }
}
