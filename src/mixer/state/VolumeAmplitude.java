package mixer.state;

import java.util.Collection;

public class VolumeAmplitude {
    public final Double volume;
    public final Double amplitude;

    public VolumeAmplitude(Double volume, Double amplitude) {
        this.volume = volume;
        this.amplitude = amplitude;
    }

    public static VolumeAmplitude sum(Collection<VolumeAmplitude> volumeAmplitudeCollection) {
        VolumeAmplitude total = new VolumeAmplitude(0., 0.);
        for(VolumeAmplitude element : volumeAmplitudeCollection){
            total = total.add(element.volume);
        }
        return total;
    }

    public VolumeAmplitude add(Double other) {
        return new VolumeAmplitude(volume + other, amplitude);
    }

    public double getValue() {
        return volume * amplitude;
    }
}
