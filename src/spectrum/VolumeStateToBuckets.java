package spectrum;

import frequency.Frequency;
import mixer.state.VolumeState;
import spectrum.buckets.AtomicBucket;
import spectrum.buckets.Bucket;
import spectrum.buckets.Buckets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VolumeStateToBuckets {

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
