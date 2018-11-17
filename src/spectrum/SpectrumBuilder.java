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

public class SpectrumBuilder {
    private final SpectrumWindow spectrumWindow;

    private final InputPort<Iterator<Map.Entry<Harmonic, Double>>> harmonicsIteratorInput;
    private final Map<Integer, OutputPort<AtomicBucket>> harmonicsOutput;

    private final TickRunner tickRunner = new MyTickRunner();

    public SpectrumBuilder(BoundedBuffer<Pulse> frameTickBuffer, BoundedBuffer<VolumeAmplitudeState> inputBuffer, SimpleBuffer<Buckets> noteOutputBuffer, SimpleBuffer<Buckets> harmonicOutputBuffer, SpectrumWindow spectrumWindow, int width) {
        this.spectrumWindow = spectrumWindow;

        LinkedList<BoundedBuffer<Pulse>> tickBroadcast = new LinkedList<>(frameTickBuffer.broadcast(2, "spectrum builder tick - broadcast"));
        LinkedList<BoundedBuffer<VolumeState>> volumeBroadcast =
            new LinkedList<>(
                tickBroadcast.poll()
                .performMethod(TimedConsumer.consumeFrom(inputBuffer), "consume from input buffer")
                .performMethod(VolumeAmplitudeToVolumeFilter::filter, "volume amplitude filter to volume")
                .broadcast(2, "Spectrum builder volume - broadcast"));

        volumeBroadcast.poll()
        .performMethod(VolumeStateToBuckets.build(spectrumWindow), "build volume state to buckets")
        .relayTo(noteOutputBuffer);

        harmonicsIteratorInput =
            volumeBroadcast.poll()
            .performMethod(HarmonicCalculator.calculateHarmonics(100), "calculate harmonics iterator")
            .createInputPort();

        harmonicsOutput = new HashMap<>();
        for (Integer i = 0; i < width; i++) {
            harmonicsOutput.put(i, new OutputPort<>("harmonic bucket"));
        }

        Map<Integer, BoundedBuffer<AtomicBucket>> harmonicsMap = new HashMap<>();
        for (Integer index : harmonicsOutput.keySet()) {
            harmonicsMap.put(index, harmonicsOutput.get(index).getBuffer().resize(1000));
        }

        tickBroadcast.poll()
        .connectTo(BuffersToBuckets.buildPipe(harmonicsMap))
        .connectTo(PrecalculatedBucketHistoryComponent.buildPipe(200))
        .relayTo(harmonicOutputBuffer);

        start();
    }

    private class MyTickRunner extends TickRunner {

        @Override
        protected void tick() {
            SpectrumBuilder.this.tick();
        }

    }

    protected void start() {
        tickRunner.start();
    }

    protected void tick() {
        try {
            Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator = harmonicsIteratorInput.consume();

            while (harmonicsIteratorInput.isEmpty()) {
                if (!update(harmonicHierarchyIterator)) break;
            }
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