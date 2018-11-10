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

public class SpectrumBuilder extends Tickable {
    private final SpectrumWindow spectrumWindow;

    private final InputPort<Iterator<Map.Entry<Harmonic, Double>>> harmonicsInput;
    private final Map<Integer, OutputPort<AtomicBucket>> harmonicsOutput;

    public SpectrumBuilder(BoundedBuffer<Pulse> frameTickBuffer, BoundedBuffer<VolumeAmplitudeState> inputBuffer, BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets,Buckets>> outputBuffer, int width, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;

        int capacity = 1;

        BoundedBuffer<Pulse>[] tickBroadcast = frameTickBuffer.broadcast(2).toArray(new BoundedBuffer[0]);
        BoundedBuffer<Pulse> frameTickBuffer1 = tickBroadcast[0];
        BoundedBuffer<Pulse> frameTickBuffer2 = tickBroadcast[1];

        BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateBuffer2 = new BoundedBuffer<>(capacity, "spectrum volume amplitude");
        new IntegratedTimedConsumerComponent<>(frameTickBuffer1, inputBuffer, volumeAmplitudeStateBuffer2);

        BoundedBuffer<VolumeState>[] volumeBroadcast =
            volumeAmplitudeStateBuffer2
            .performMethod(VolumeAmplitudeToVolumeFilter::filter)
            .broadcast(2).toArray(new BoundedBuffer[0]);
        BoundedBuffer<VolumeState> volumeStateBuffer2 = volumeBroadcast[0];
        BoundedBuffer<VolumeState> volumeStateBuffer3 = volumeBroadcast[1];

        BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> harmonicsBuffer = new BoundedBuffer<>(capacity, "spectrum harmonics");
        new HarmonicCalculator(100, volumeStateBuffer3, harmonicsBuffer);

        Map<Integer, BoundedBuffer<AtomicBucket>> harmonicsMap = new HashMap<>();
        for(Integer i = 0; i< width; i++){
            harmonicsMap.put(i, new BoundedBuffer<>(1000, "harmonics bucket"));
        }

        harmonicsInput = new InputPort<>(harmonicsBuffer);
        harmonicsOutput = new HashMap<>();
        for(Integer index : harmonicsMap.keySet()){
            harmonicsOutput.put(index, new OutputPort<>(harmonicsMap.get(index)));
        }

        BoundedBuffer<Buckets> inputHarmonicsBucketsBuffer = new BoundedBuffer<>(1, "Input harmonics buffer");
        new BuffersToBuckets(harmonicsMap, frameTickBuffer2, inputHarmonicsBucketsBuffer);
        BoundedBuffer<Buckets> harmonicSpectrumBuffer = new BoundedBuffer<>(capacity, "spectrum - harmonics buckets");
        new PrecalculatedBucketHistoryComponent(inputHarmonicsBucketsBuffer, harmonicSpectrumBuffer, 200);

        volumeStateBuffer2
        .performMethod(VolumeStateToBuckets.toBuckets(spectrumWindow))
        .pairWith(harmonicSpectrumBuffer)
        .relayTo(outputBuffer);

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