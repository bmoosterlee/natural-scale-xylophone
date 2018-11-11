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

import java.util.*;

public class SpectrumBuilder extends TickablePipeComponent {
    public SpectrumBuilder(BoundedBuffer<Pulse> frameTickBuffer, BoundedBuffer<VolumeAmplitudeState> inputBuffer, SimpleBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> outputBuffer, SpectrumWindow spectrumWindow, int width) {
        super(frameTickBuffer, outputBuffer, build(inputBuffer, spectrumWindow, width));
    }

    public static CallableWithArguments<Pulse, AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> build(BoundedBuffer<VolumeAmplitudeState> inputBuffer, SpectrumWindow spectrumWindow1, int width) {
        return new CallableWithArguments<>() {
            private final InputPort<Iterator<Map.Entry<Harmonic, Double>>> harmonicsInput;
            private final Map<Integer, OutputPort<AtomicBucket>> harmonicsOutput;

            private final SpectrumWindow spectrumWindow;
            private final OutputPort<Pulse> methodInput;
            private final InputPort<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> methodOutput;

            {
                this.spectrumWindow = spectrumWindow1;

                BoundedBuffer<Pulse> relayFrameTickBuffer = new SimpleBuffer<>(1000, "SpecturmBuilder - relayFrameTick");
                methodInput = relayFrameTickBuffer.createOutputPort();
                BoundedBuffer<Pulse>[] tickBroadcast = relayFrameTickBuffer.broadcast(2).toArray(new BoundedBuffer[0]);
                BoundedBuffer<Pulse> frameTickBuffer1 = tickBroadcast[0];
                BoundedBuffer<Pulse> frameTickBuffer2 = tickBroadcast[1];

                BoundedBuffer<VolumeState>[] volumeBroadcast =
                        frameTickBuffer1
                                .performMethod(IntegratedTimedConsumerComponent.consumeFrom(inputBuffer))
                                .performMethod(VolumeAmplitudeToVolumeFilter::filter)
                                .broadcast(2).toArray(new BoundedBuffer[0]);
                BoundedBuffer<VolumeState> volumeStateBuffer2 = volumeBroadcast[0];
                BoundedBuffer<VolumeState> volumeStateBuffer3 = volumeBroadcast[1];

                harmonicsInput =
                        volumeStateBuffer3
                                .performMethod(HarmonicCalculator.calculateHarmonics(100))
                                .createInputPort();

                Map<Integer, BoundedBuffer<AtomicBucket>> harmonicsMap = new HashMap<>();
                for (Integer i = 0; i < width; i++) {
                    harmonicsMap.put(i, new SimpleBuffer<>(1000, "harmonics bucket"));
                }

                harmonicsOutput = new HashMap<>();
                for (Integer index : harmonicsMap.keySet()) {
                    harmonicsOutput.put(index, new OutputPort<>(harmonicsMap.get(index)));
                }

                methodOutput = volumeStateBuffer2
                        .performMethod(VolumeStateToBuckets.toBuckets(spectrumWindow))
                        .pairWith(
                                frameTickBuffer2
                                    .performMethod(BuffersToBuckets.toBuckets(harmonicsMap))
                                    .performMethod(PrecalculatedBucketHistoryComponent.recordHistory(200)))
                        .createInputPort();
            }

            private AbstractMap.SimpleImmutableEntry<Buckets, Buckets> buildSpectrum(Pulse input) {
                try {
                    methodInput.produce(input);

                    Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator = harmonicsInput.consume();
                    while (harmonicsInput.isEmpty()) {
                        if (!update(harmonicHierarchyIterator)) break;
                    }
                    AbstractMap.SimpleImmutableEntry<Buckets, Buckets> result = methodOutput.consume();

                    return result;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return null;
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


            @Override
            public AbstractMap.SimpleImmutableEntry<Buckets, Buckets> call(Pulse input) {
                return buildSpectrum(input);
            }
        };
    }
}