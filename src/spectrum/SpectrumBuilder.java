package spectrum;

import component.*;
import component.buffer.*;
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

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;

public class SpectrumBuilder {

    public static SimpleImmutableEntry<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> buildComponent(BoundedBuffer<Pulse> frameTickBuffer, BoundedBuffer<VolumeAmplitudeState> inputBuffer, SpectrumWindow spectrumWindow, int width) {
        LinkedList<BoundedBuffer<Pulse>> tickBroadcast = new LinkedList<>(frameTickBuffer.broadcast(2, "spectrum builder tick - broadcast"));
        LinkedList<BoundedBuffer<VolumeState>> volumeBroadcast =
            new LinkedList<>(
                tickBroadcast.poll()
                .performMethod(TimedConsumer.consumeFrom(inputBuffer), "consume from input buffer")
                .performMethod(VolumeAmplitudeToVolumeFilter::filter, "volume amplitude filter to volume")
                .broadcast(2, "Spectrum builder volume - broadcast"));

        BoundedBuffer<Buckets> noteOutputBuffer =
            volumeBroadcast.poll()
            .performMethod(VolumeStateToBuckets.build(spectrumWindow), "build volume state to buckets");

        InputPort<Iterator<Map.Entry<Harmonic, Double>>> harmonicsIteratorInput =
            volumeBroadcast.poll()
            .performMethod(HarmonicCalculator.calculateHarmonics(100), "calculate harmonics iterator")
            .createInputPort();

        Map<Integer, OutputPort<AtomicBucket>> harmonicsOutput = new HashMap<>();
        for (Integer i = 0; i < width; i++) {
            harmonicsOutput.put(i, new OutputPort<>("harmonic bucket"));
        }

        Map<Integer, BoundedBuffer<AtomicBucket>> harmonicsMap = new HashMap<>();
        for (Integer index : harmonicsOutput.keySet()) {
            harmonicsMap.put(index, harmonicsOutput.get(index).getBuffer().resize(1000));
        }

        BoundedBuffer<Buckets> harmonicsOutputBuffer =
            tickBroadcast.poll()
            .connectTo(BuffersToBuckets.buildPipe(harmonicsMap))
            .connectTo(PrecalculatedBucketHistoryComponent.buildPipe(200));

        new TickRunner(){

            @Override
            protected void tick() {
                try {
                    Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator = harmonicsIteratorInput.consume();

                    while (harmonicsIteratorInput.isEmpty()) {
                        if (!update(harmonicHierarchyIterator, harmonicsOutput, spectrumWindow)) break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }.start();

        return new SimpleImmutableEntry<>(noteOutputBuffer, harmonicsOutputBuffer);
    }


    //return true when harmonicHierarchy has been depleted.
    private static boolean update(Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator, Map<Integer, OutputPort<AtomicBucket>> harmonicsOutput, SpectrumWindow spectrumWindow) {
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