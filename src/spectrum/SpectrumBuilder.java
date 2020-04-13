package spectrum;

import component.*;
import component.buffer.*;
import frequency.Frequency;
import sound.VolumeState;
import spectrum.buckets.*;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.stream.Collectors;

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

        int maxHarmonics = 200;
        return volumeBroadcast.poll()
                .performMethod(input -> new Pulse(), "harmonic spectrum - to pulse")
                .performMethod(Flusher.flush(
                        (SimpleBuffer<SimpleImmutableEntry<Integer, AtomicBucket>, Packet<SimpleImmutableEntry<Integer, AtomicBucket>>>)
                                calculateHarmonicsContinuously(
                                    volumeBroadcast.poll()
                                            .performMethod(HarmonicCalculator.calculateHarmonics(maxHarmonics), "harmonic spectrum - build harmonics iterator"), maxHarmonics)
                                    .performMethod(harmonicWithVolume -> {
                                        Frequency frequency = harmonicWithVolume.getKey().getHarmonicFrequency();
                                        return new SimpleImmutableEntry<>(frequency, harmonicWithVolume.getValue());
                                    }, maxHarmonics, "harmonic spectrum - extract harmonic")
                                    .performMethod(input1 -> new SimpleImmutableEntry<>(input1.getKey(), new AtomicBucket(input1.getKey(), input1.getValue())), maxHarmonics, "harmonic spectrum - build bucket")
                                    .performMethod(input1 -> new SimpleImmutableEntry<>(spectrumWindow.getX(input1.getKey()), input1.getValue()), maxHarmonics, "harmonic spectrum - frequency to integer")
                                    .connectTo(spectrumWindow.buildInBoundsFilterPipe(maxHarmonics))
                                    .relayTo(new SimpleBuffer<>(new OverflowStrategy<>("harmonic spectrum - finished bucket list")))),
                        "harmonic spectrum - flush new harmonic buckets")
                .performMethod(input -> new Buckets(input.stream().collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue, Bucket::add))), "spectrum builder - bucket list to buckets")
                .performMethod(PrecalculatedBucketHistory.build(200), "spectrum builder - harmonic spectrum history");
    }

    private static <A extends Packet<Map.Entry<Harmonic, Double>>, B extends Packet<Iterator<Map.Entry<Harmonic, Double>>>> BoundedBuffer<Map.Entry<Harmonic, Double>, A> calculateHarmonicsContinuously(BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>, B> harmonicsIteratorBuffer, int maxHarmonics) {
        SimpleBuffer<Map.Entry<Harmonic, Double>, A> outputBuffer = new SimpleBuffer<>(maxHarmonics, "spectrum builder - harmonic calculation");

        new TickRunningStrategy(new AbstractPipeComponent<>(harmonicsIteratorBuffer.createInputPort(), outputBuffer.createOutputPort()){
            @Override
            protected void tick() {
                try {
                    B harmonicsIterator = input.consume();
                    while (input.isEmpty() && harmonicsIterator.unwrap().hasNext()) {
                        output.produce(harmonicsIterator.transform(Iterator::next));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        return outputBuffer;
    }

}