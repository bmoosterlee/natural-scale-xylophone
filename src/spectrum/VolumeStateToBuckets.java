package spectrum;

import component.buffer.*;
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

public class VolumeStateToBuckets extends TickRunner {
    private final CallableWithArguments<VolumeState, Buckets> bucketBuilder;

    private final InputPort<VolumeState> volumeStateInput;
    private final OutputPort<Buckets> notesBucketsOutput;

    public VolumeStateToBuckets(BoundedBuffer<VolumeState> volumeStateBuffer, SimpleBuffer<Buckets> notesBucketsBuffer, SpectrumWindow spectrumWindow) {
        bucketBuilder = toBuckets(spectrumWindow);

        volumeStateInput = new InputPort<>(volumeStateBuffer);
        notesBucketsOutput = new OutputPort<>(notesBucketsBuffer);

        start();
    }

    protected void tick(){
        try {
            VolumeState volumeState = volumeStateInput.consume();

            Buckets noteBuckets = bucketBuilder.call(volumeState);

            notesBucketsOutput.produce(noteBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
