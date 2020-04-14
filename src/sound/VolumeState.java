package sound;

public class VolumeState {

    public final Double[] volumes;

    public VolumeState(Double[] volumes) {
        this.volumes = volumes;
    }

    public VolumeState add(VolumeState other) {
        Double[] newVolumes = new Double[volumes.length];

        for(int i = 0; i < volumes.length; i++){
            newVolumes[i] = volumes[i] + other.volumes[i];
        }

        return new VolumeState(newVolumes);
    }
}
