package sound;

public class VolumeAmplitude {
    private final Double volume;
    private final Double amplitude;

    VolumeAmplitude(Double volume, Double amplitude) {
        if(volume!=null) {
            this.volume = volume;
        } else {
            this.volume = 0.;
        }
        this.amplitude = amplitude;
    }

    public double getValue() {
        return volume * amplitude;
    }
}
