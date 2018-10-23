package notes.state;

//todo move to composite volume. Do this by having a volume object which either has atomic volume (value) or composite volume (set)
public class VolumeAmplitude {
    public final Double volume;
    private final Double amplitude;

    VolumeAmplitude(Double volume, Double amplitude) {
        this.volume = volume;
        this.amplitude = amplitude;
    }

    public VolumeAmplitude add(Double other) {
        return new VolumeAmplitude(volume + other, amplitude);
    }

    public double getValue() {
        return volume * amplitude;
    }
}
