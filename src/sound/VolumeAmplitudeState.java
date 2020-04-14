package sound;

import java.util.Arrays;

public class VolumeAmplitudeState {

    public final VolumeAmplitude[] volumeAmplitudes;

    public VolumeAmplitudeState(VolumeState volumeState, AmplitudeState amplitudeState) {
        volumeAmplitudes = new VolumeAmplitude[amplitudeState.amplitudes.length];
        for (int i = 0; i < amplitudeState.amplitudes.length; i++) {
            volumeAmplitudes[i] = new VolumeAmplitude(
                    volumeState.volumes[i],
                    amplitudeState.amplitudes[i]);
        }
    }

    public Double toDouble() {
        return Arrays.stream(volumeAmplitudes)
                .map(VolumeAmplitude::getValue)
                .reduce(0., Double::sum);
    }
}
