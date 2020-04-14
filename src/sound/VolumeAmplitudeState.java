package sound;

import java.util.Arrays;

public class VolumeAmplitudeState {

    public final VolumeAmplitude[] volumeAmplitudes;

    public VolumeAmplitudeState(Double[] volumeState, Double[] amplitudeState) {
        volumeAmplitudes = new VolumeAmplitude[amplitudeState.length];
        for (int i = 0; i < amplitudeState.length; i++) {
            volumeAmplitudes[i] = new VolumeAmplitude(
                    volumeState[i],
                    amplitudeState[i]);
        }
    }

    public Double toDouble() {
        return Arrays.stream(volumeAmplitudes)
                .map(VolumeAmplitude::getValue)
                .reduce(0., Double::sum);
    }
}
