package spectrum;

import component.Pulse;
import component.TimedConsumer;
import component.buffer.*;
import frequency.Frequency;
import mixer.state.VolumeState;
import spectrum.buckets.AtomicBucket;
import spectrum.buckets.Buckets;
import spectrum.buckets.BuffersToBuckets;
import spectrum.buckets.PrecalculatedBucketHistoryComponent;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;

public class SpectrumBuilder {

    public static SimpleImmutableEntry<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> buildComponent(BoundedBuffer<Pulse> frameTickBuffer, BoundedBuffer<VolumeState> inputBuffer, SpectrumWindow spectrumWindow, int width) {
        LinkedList<BoundedBuffer<Pulse>> tickBroadcast = new LinkedList<>(frameTickBuffer.broadcast(2, "spectrum builder tick - broadcast"));
        LinkedList<BoundedBuffer<VolumeState>> volumeBroadcast =
            new LinkedList<>(
                tickBroadcast.poll()
                .performMethod(TimedConsumer.consumeFrom(inputBuffer), "consume from input buffer")
                .broadcast(2, "Spectrum builder volume - broadcast"));

        BoundedBuffer<Buckets> noteOutputBuffer =
            volumeBroadcast.poll()
            .performMethod(input -> new Buckets(input, spectrumWindow), "build volume state to buckets");


        Map<Integer, OutputPort<AtomicBucket>> harmonicsOutput = new HashMap<>();
        for (Integer i = 0; i < width; i++) {
            harmonicsOutput.put(i, new OutputPort<>("harmonic bucket"));
        }

        BufferChainLink<Iterator<Map.Entry<Harmonic, Double>>> harmonicsIteratorBuffer = volumeBroadcast.poll()
                .performMethod(HarmonicCalculator.calculateHarmonics(100), "calculate harmonics iterator");

        harmonicsIteratorBuffer.performInputMethod(input -> {
                    while (harmonicsIteratorBuffer.isEmpty()) {
                        if (!update(input, harmonicsOutput, spectrumWindow)) break;
                    }
                });

        Map<Integer, BoundedBuffer<AtomicBucket>> harmonicsMap = new HashMap<>();
        for (Integer index : harmonicsOutput.keySet()) {
            harmonicsMap.put(index, harmonicsOutput.get(index).getBuffer().resize(1000));
        }

        BoundedBuffer<Buckets> harmonicsOutputBuffer =
            tickBroadcast.poll()
            .connectTo(BuffersToBuckets.buildPipe(harmonicsMap))
            .connectTo(PrecalculatedBucketHistoryComponent.buildPipe(200));

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