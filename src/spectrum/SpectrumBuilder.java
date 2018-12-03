package spectrum;

import component.Mapper;
import component.Pulse;
import component.Separator;
import component.TimedConsumer;
import component.buffer.*;
import frequency.Frequency;
import mixer.state.VolumeState;
import spectrum.buckets.AtomicBucket;
import spectrum.buckets.Buckets;
import spectrum.buckets.HarmonicBucketsUnmapper;
import spectrum.buckets.PrecalculatedBucketHistoryComponent;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpectrumBuilder {

    public static <A extends Packet<Buckets>, B extends Packet<VolumeState>> SimpleImmutableEntry<BoundedBuffer<Buckets, A>, BoundedBuffer<Buckets, SimplePacket<Buckets>>> buildComponent(BoundedBuffer<Pulse, ? extends Packet<Pulse>> frameTickBuffer, BoundedBuffer<VolumeState, B> inputBuffer, SpectrumWindow spectrumWindow) {
        BufferChainLink<VolumeState, B> volumeStatePacketBufferChainLink = frameTickBuffer
                .performMethod(TimedConsumer.consumeFrom(inputBuffer), "spectrum builder - consume from volume state buffer");
        LinkedList<BoundedBuffer<VolumeState, B>> volumeBroadcast =
                new LinkedList<>(
                        volumeStatePacketBufferChainLink
                                .broadcast(2, "spectrum builder - volume broadcast"));

        return new SimpleImmutableEntry<>(
                buildNoteSpectrumPipe(volumeBroadcast.poll(), spectrumWindow),
                buildHarmonicSpectrumPipe(volumeBroadcast.poll(), spectrumWindow));
    }

    private static <A extends Packet<Buckets>, B extends Packet<VolumeState>> BufferChainLink<Buckets, A> buildNoteSpectrumPipe(BoundedBuffer<VolumeState, B> inputBuffer, SpectrumWindow spectrumWindow) {
        return inputBuffer
                .performMethod(input -> new Buckets(input, spectrumWindow), "build note spectrum");
    }

    private static <B extends Packet<VolumeState>> BoundedBuffer<Buckets, SimplePacket<Buckets>> buildHarmonicSpectrumPipe(BoundedBuffer<VolumeState, B> volumeBuffer, SpectrumWindow spectrumWindow) {
        LinkedList<BoundedBuffer<VolumeState, B>> volumeBroadcast = new LinkedList<>(volumeBuffer.broadcast(2, "build harmonic spectrum - tick broadcast"));

        return volumeBroadcast.poll()
                .performMethod(input -> new Pulse(), "harmonic spectrum - to pulse")
                .connectTo(
                        HarmonicBucketsUnmapper.buildPipe(
                                Mapper.buildComponent(
                                        calculateHarmonicsContinuously(
                                                volumeBroadcast.poll()
                                                .performMethod(HarmonicCalculator.calculateHarmonics(100), "harmonic spectrum - build harmonics iterator"))
                                        .connectTo((PipeCallable<BoundedBuffer<Collection<Map.Entry<Harmonic, Double>>, Packet<Collection<Map.Entry<Harmonic, Double>>>>, BoundedBuffer<Map.Entry<Harmonic, Double>, Packet<Map.Entry<Harmonic, Double>>>>) Separator::separate)
                                        .performMethod(harmonicWithVolume -> {
                                            Frequency frequency = harmonicWithVolume.getKey().getHarmonicFrequency();
                                            return new SimpleImmutableEntry<>(frequency, harmonicWithVolume.getValue());}, "harmonic spectrum - extract harmonic")
                                        .performMethod(input -> new SimpleImmutableEntry<>(input.getKey(), new AtomicBucket(input.getKey(), input.getValue())), "harmonic spectrum - build bucket")
                                        .performMethod(input -> new SimpleImmutableEntry<>(spectrumWindow.getX(input.getKey()), input.getValue()), "harmonic spectrum - frequency to integer")
                                        .connectTo(spectrumWindow.buildInBoundsFilterPipe()),
                                IntStream.range(0, spectrumWindow.width).boxed().collect(Collectors.toSet()))))
                .connectTo(PrecalculatedBucketHistoryComponent.buildPipe(200));
    }

    private static <A extends Packet<Collection<Map.Entry<Harmonic, Double>>>, B extends Packet<Iterator<Map.Entry<Harmonic, Double>>>> BufferChainLink<Collection<Map.Entry<Harmonic, Double>>, A> calculateHarmonicsContinuously(BufferChainLink<Iterator<Map.Entry<Harmonic, Double>>, B> harmonicsIteratorBuffer) {
        return harmonicsIteratorBuffer
                .performMethod(
                    harmonicsIterator -> {
                        HashSet<Map.Entry<Harmonic, Double>> newHarmonics = new HashSet<>();
                        while (harmonicsIteratorBuffer.isEmpty() && harmonicsIterator.hasNext()) {
                            newHarmonics.add(harmonicsIterator.next());
                        }
                        return newHarmonics;
                }, "harmonic spectrum - calculate harmonics continuously");
    }

}