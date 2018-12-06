package mixer.state;

public class VolumeAmplitude {
    private final Double volume;
    private final Double amplitude;

    VolumeAmplitude(Double volume, Double amplitude) {
        this.volume = volume;
        this.amplitude = amplitude;
    }

    public double getValue() {
        return volume * amplitude;
    }
}
