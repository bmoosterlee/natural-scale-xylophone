package component;

import component.buffer.*;
import spectrum.SpectrumWindow;

public class Memorizer {

    public static <K, A extends Packet<K>, V, B extends Packet<V>> PipeCallable<BoundedBuffer<K, A>, BoundedBuffer<V, B>> buildPipe(BoundedBuffer<V, B> memorizedBuffer, String name) {
        return inputBuffer -> {
            SimpleBuffer<V, B> outputBuffer = new SimpleBuffer<>(1, name);
            new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {

                B memory;

                @Override
                protected void tick() {
                    try {
                        input.consume();
                        B audioInPacket;
                        if (memory == null) {
                            memory = memorizedBuffer.poll();
                        } else {
                            audioInPacket = memorizedBuffer.tryPoll();
                            if (audioInPacket != null) {
                                memory = audioInPacket;
                            }
                        }
                        output.produce(memory);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            return outputBuffer;
        };
    }

    public static <K, A extends Packet<K>> PipeCallable<BoundedBuffer<K, A>, BoundedBuffer<Double[], SimplePacket<Double[]>>> buildInterpolator(BoundedBuffer<Double[], SimplePacket<Double[]>> memorizedBuffer, String name, SpectrumWindow spectrumWindow, int frameRate) {
        return inputBuffer -> {
            SimpleBuffer<Double[], SimplePacket<Double[]>> outputBuffer = new SimpleBuffer<>(1, name);
            new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {

                Double[] memory;
                Double[] memoryNew;

                long count = 0;

                {
                    memoryNew = new Double[spectrumWindow.width];
                    for(int i = 0; i<spectrumWindow.width; i++){
                        memoryNew[i] = 0.;
                    }
                }

                @Override
                protected void tick() {
                    try {
                        input.consume();
                        if (memory == null) {
                            memory = memoryNew;
                            memoryNew = memorizedBuffer.poll().unwrap();
                            output.produce(new SimplePacket<>(memoryNew));
                        }
                        SimplePacket<Double[]> audioPacket = memorizedBuffer.tryPoll();
                        if (audioPacket != null) {
                            memory = memoryNew;
                            memoryNew = audioPacket.unwrap();
                            count = 0;

                            output.produce(new SimplePacket<>(memory));
                        } else {
                            Double[] interpolatedMagnitudes = new Double[spectrumWindow.width];
                            double completion = (double) count / (frameRate / 2.);
                            completion = Math.max(completion, 1.);

                            for (int i = 0; i < spectrumWindow.width; i++) {
                                interpolatedMagnitudes[i] = (1. - completion) * memory[i] + completion * memoryNew[i];
                            }
                            count++;

                            output.produce(new SimplePacket<>(interpolatedMagnitudes));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            return outputBuffer;
        };
    }
}
