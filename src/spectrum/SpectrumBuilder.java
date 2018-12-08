package spectrum;

import component.*;
import component.buffer.*;
import frequency.Frequency;
import mixer.state.VolumeState;
import spectrum.buckets.*;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
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
                .performMethod(Flusher.flush(
                        calculateHarmonicsContinuously(
                                volumeBroadcast.poll()
                                        .performMethod(HarmonicCalculator.calculateHarmonics(100), "harmonic spectrum - build harmonics iterator"))
                                .performMethod(harmonicWithVolume -> {
                                    Frequency frequency = harmonicWithVolume.getKey().getHarmonicFrequency();
                                    return new SimpleImmutableEntry<>(frequency, harmonicWithVolume.getValue());
                                    }, "harmonic spectrum - extract harmonic")
                                .performMethod(input -> new SimpleImmutableEntry<>(input.getKey(), new AtomicBucket(input.getKey(), input.getValue())), "harmonic spectrum - build bucket")
                                .performMethod(input -> new SimpleImmutableEntry<>(spectrumWindow.getX(input.getKey()), input.getValue()), "harmonic spectrum - frequency to integer")
                                .connectTo(spectrumWindow.buildInBoundsFilterPipe())
                                .toOverwritable()
                                .resize(1000)))
                .performMethod(input -> new Buckets(input.stream().collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue, Bucket::add))), "spectrum builder - bucket list to buckets")
                .connectTo(PrecalculatedBucketHistoryComponent.buildPipe(200));
    }

    private static <A extends Packet<Map.Entry<Harmonic, Double>>, B extends Packet<Iterator<Map.Entry<Harmonic, Double>>>> BoundedBuffer<Map.Entry<Harmonic, Double>, A> calculateHarmonicsContinuously(BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>, B> harmonicsIteratorBuffer) {
        SimpleBuffer<Map.Entry<Harmonic, Double>, A> outputBuffer = new SimpleBuffer<>(1, "spectrum builder - harmonic calculation");

        new TickRunningStrategy(new AbstractPipeComponent<>(harmonicsIteratorBuffer.createInputPort(), outputBuffer.createOutputPort()){
            @Override
            protected void tick() {
                try {
                    B harmonicsIterator = input.consume();
                    while (input.isEmpty() && harmonicsIterator.unwrap().hasNext()) {
                        output.produce(harmonicsIterator.transform(input -> input.next()));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public Boolean isParallelisable() {
                return false;
            }
        });

        return outputBuffer;
    }

}