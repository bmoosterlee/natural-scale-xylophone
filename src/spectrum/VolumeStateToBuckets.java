package spectrum;

import component.buffer.*;
import component.utilities.RunningPipeComponent;
import component.utilities.TickRunner;
import frequency.Frequency;
import spectrum.buckets.AtomicBucket;
import spectrum.buckets.Bucket;
import spectrum.buckets.Buckets;
import mixer.state.VolumeState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VolumeStateToBuckets extends RunningPipeComponent<VolumeState, Buckets> {

    public VolumeStateToBuckets(BoundedBuffer<VolumeState> volumeStateBuffer, SimpleBuffer<Buckets> notesBucketsBuffer, SpectrumWindow spectrumWindow) {
        super(volumeStateBuffer, notesBucketsBuffer, toBuckets(spectrumWindow));
    }

    public static CallableWithArguments<VolumeState, Buckets> toBuckets(SpectrumWindow spectrumWindow){
        return new CallableWithArguments<>() {
            SpectrumWindow spectrumWindow1 = spectrumWindow;

            @Override
            public Buckets call(VolumeState input) {
                return toBuckets(input, spectrumWindow1);
            }
        };
    }

    public static Buckets toBuckets(VolumeState volumeState, SpectrumWindow spectrumWindow){
        Map<Frequency, Double> volumes = volumeState.volumes;
        Set<Frequency> keys = volumes.keySet();

        Set<Integer> indices = new HashSet<>();
        Map<Integer, Bucket> entries = new HashMap<>();

        for(Frequency frequency : keys){
            int x = spectrumWindow.getX(frequency);

            indices.add(x);
            entries.put(x, new AtomicBucket(frequency, volumes.get(frequency)));
        }

        return new Buckets(indices, entries);
    }
}
