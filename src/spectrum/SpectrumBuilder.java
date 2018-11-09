package spectrum;

import component.*;
import frequency.Frequency;
import spectrum.buckets.AtomicBucket;
import spectrum.buckets.Buckets;
import spectrum.buckets.BuffersToBuckets;
import spectrum.buckets.PrecalculatedBucketHistoryComponent;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;
import component.Pulse;
import mixer.state.VolumeAmplitudeState;
import mixer.state.VolumeAmplitudeToVolumeFilter;
import mixer.state.VolumeState;
import time.PerformanceTracker;
import time.TimeKeeper;

import java.util.*;

public class SpectrumBuilder extends TickingComponent {
    private final SpectrumWindow spectrumWindow;

    private final InputPort<Iterator<Map.Entry<Harmonic, Double>>> harmonicsInput;
    private final Map<Integer, OutputPort<AtomicBucket>> harmonicsOutput;

    public SpectrumBuilder(BoundedBuffer<Pulse> frameTickBuffer, BoundedBuffer<VolumeAmplitudeState> inputBuffer, BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets,Buckets>> outputBuffer, int width, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;

        int capacity = 1;

        BoundedBuffer<Buckets> noteSpectrumBuffer = new BoundedBuffer<>(capacity, "spectrum - note buckets");
        BoundedBuffer<Buckets> harmonicSpectrumBuffer = new BoundedBuffer<>(capacity, "spectrum - harmonics buckets");
        new Pairer<>(noteSpectrumBuffer, harmonicSpectrumBuffer, outputBuffer);

        BoundedBuffer<Pulse> frameTickBuffer1 = new BoundedBuffer<>(capacity,"spectrum frame tick 1");
        BoundedBuffer<Pulse> frameTickBuffer2 = new BoundedBuffer<>(capacity, "spectrum frame tick 2");
        new Broadcast<>(frameTickBuffer, Arrays.asList(frameTickBuffer1, frameTickBuffer2));

        BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer2 = new BoundedBuffer<>(capacity, "spectrum volume amplitude");
        new IntegratedTimedConsumerComponent<>(frameTickBuffer1, inputBuffer, volumeAmplitudeStateBuffer2);
        BoundedBuffer<VolumeState> volumeStateBuffer = new BoundedBuffer<>(capacity, "spectrum volume");
        new VolumeAmplitudeToVolumeFilter(volumeAmplitudeStateBuffer2, volumeStateBuffer);

        BoundedBuffer<VolumeState> volumeStateBuffer2 = new BoundedBuffer<>(capacity, "volumeState 2");
        BoundedBuffer<VolumeState> volumeStateBuffer3 = new BoundedBuffer<>(capacity, "volumeState 3");
        new Broadcast<>(volumeStateBuffer, new HashSet<>(Arrays.asList(volumeStateBuffer2, volumeStateBuffer3)));

        new VolumeStateToBuckets(spectrumWindow, volumeStateBuffer2, noteSpectrumBuffer);

        BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> harmonicsBuffer = new BoundedBuffer<>(capacity, "spectrum harmonics");
        new HarmonicCalculator(100, volumeStateBuffer3, harmonicsBuffer);

        Map<Integer, BoundedBuffer<AtomicBucket>> harmonicsMap = new HashMap<>();
        for(Integer i = 0; i< width; i++){
            harmonicsMap.put(i, new BoundedBuffer<>(1000, "harmonics bucket"));
        }

        BoundedBuffer<Buckets> inputHarmonicsBucketsBuffer = new BoundedBuffer<>(1, "Input harmonics buffer");
        new BuffersToBuckets(harmonicsMap, frameTickBuffer2, inputHarmonicsBucketsBuffer);
        new PrecalculatedBucketHistoryComponent(inputHarmonicsBucketsBuffer, harmonicSpectrumBuffer, 200);

        harmonicsInput = new InputPort<>(harmonicsBuffer);
        harmonicsOutput = new HashMap<>();
        for(Integer index : harmonicsMap.keySet()){
            harmonicsOutput.put(index, new OutputPort<>(harmonicsMap.get(index)));
        }

        start();
    }

    @Override
    protected void tick() {
        try {
            Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator = harmonicsInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
            while (harmonicsInput.isEmpty()) {
                if (!update(harmonicHierarchyIterator)) break;
            }
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //return true when harmonicHierarchy has been depleted.
    private boolean update(Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator) {
        try {
            Map.Entry<Harmonic, Double> harmonicVolume = harmonicHierarchyIterator.next();
            Frequency frequency = harmonicVolume.getKey().getHarmonicFrequency();

            AtomicBucket newBucket = new AtomicBucket(frequency, harmonicVolume.getValue());
            harmonicsOutput.get(spectrumWindow.getX(frequency)).produce(newBucket);

        } catch (NoSuchElementException e) {
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException ignored) {
            //harmonic is out of bounds during the call to harmonicsOutput.get
        }
        return true;
    }

}