package notes.state;

import java.util.Collection;
//todo move to composite volume. Do this by having a volume object which either has atomic volume (value) or composite volume (set)
public class VolumeAmplitude {
    public final Double volume;
    public final Double amplitude;

    VolumeAmplitude(Double volume, Double amplitude) {
        this.volume = volume;
        this.amplitude = amplitude;
    }

    static VolumeAmplitude sum(Collection<VolumeAmplitude> volumeAmplitudeCollection) {
        VolumeAmplitude total = null;
        for(VolumeAmplitude element : volumeAmplitudeCollection){
            try {
                total = total.add(element.volume);
            }
            catch(NullPointerException e){
                total = element;
            }
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
