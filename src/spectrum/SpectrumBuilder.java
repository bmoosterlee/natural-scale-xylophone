package spectrum;

import component.Pulse;
import component.Separator;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpectrumBuilder {

    public static SimpleImmutableEntry<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> buildComponent(BoundedBuffer<Pulse> frameTickBuffer, BoundedBuffer<VolumeState> inputBuffer, SpectrumWindow spectrumWindow) {
        LinkedList<BoundedBuffer<Pulse>> tickBroadcast = new LinkedList<>(frameTickBuffer.broadcast(2, "spectrum builder tick - broadcast"));
        LinkedList<BoundedBuffer<VolumeState>> volumeBroadcast =
            new LinkedList<>(
                tickBroadcast.poll()
                .performMethod(TimedConsumer.consumeFrom(inputBuffer), "consume from input buffer")
                .broadcast(2, "Spectrum builder volume - broadcast"));

        BufferChainLink<Iterator<Map.Entry<Harmonic, Double>>> harmonicsIteratorBuffer =
                volumeBroadcast.poll()
                .performMethod(HarmonicCalculator.calculateHarmonics(100), "calculate harmonics iterator");

        return new SimpleImmutableEntry<>(
                volumeBroadcast.poll()
                .performMethod(input -> new Buckets(input, spectrumWindow), "build volume state to buckets"),
                tickBroadcast.poll()
                .connectTo(
                        BuffersToBuckets.buildPipe(
                            Mapper.buildComponent(
                                calculateHarmonicsContinuously(harmonicsIteratorBuffer)
                                .connectTo((PipeCallable<BoundedBuffer<Collection<Map.Entry<Harmonic, Double>>>, BoundedBuffer<Map.Entry<Harmonic, Double>>>) Separator::separate)
                                .performMethod(harmonicWithVolume -> {
                                    Frequency frequency = harmonicWithVolume.getKey().getHarmonicFrequency();
                                    return new SimpleImmutableEntry<>(frequency, harmonicWithVolume.getValue());
                                })
                                .performMethod(input -> new SimpleImmutableEntry<>(input.getKey(), new AtomicBucket(input.getKey(), input.getValue())))
                                .performMethod(input -> new SimpleImmutableEntry<>(spectrumWindow.getX(input.getKey()), input.getValue()))
                                .connectTo(spectrumWindow.buildInBoundsFilterPipe()),
                            IntStream.range(0, spectrumWindow.width).boxed().collect(Collectors.toSet()))))
                .connectTo(PrecalculatedBucketHistoryComponent.buildPipe(200)));
    }

    private static BufferChainLink<Collection<Map.Entry<Harmonic, Double>>> calculateHarmonicsContinuously(BufferChainLink<Iterator<Map.Entry<Harmonic, Double>>> harmonicsIteratorBuffer) {
        return harmonicsIteratorBuffer
                .performMethod(
                    harmonicsIterator -> {
                        HashSet<Map.Entry<Harmonic, Double>> newHarmonics = new HashSet<>();
                        while (harmonicsIteratorBuffer.isEmpty() && harmonicsIterator.hasNext()) {
                            newHarmonics.add(harmonicsIterator.next());
                        }
                        return newHarmonics;
                });
    }

}