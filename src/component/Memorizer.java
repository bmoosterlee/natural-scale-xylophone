package component;

import component.buffer.*;

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
}
