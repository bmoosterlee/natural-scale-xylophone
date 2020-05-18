package component;

import component.buffer.*;

import java.util.List;
import java.util.stream.Collectors;

public class Flusher {

    public static <T, B extends Packet<T>> PipeCallable<Pulse, List<T>> flush(BoundedBuffer<T, B> inputBuffer){
        return flushInternal(inputBuffer, Flusher::flush, Packet::unwrap);
    }

    public static <T, B extends Packet<T>> PipeCallable<Pulse, List<T>> flushOrConsume(BoundedBuffer<T, B> inputBuffer){
        return flushInternal(inputBuffer, Flusher::flushOrConsume, Packet::unwrap);
    }

    static <T, B extends Packet<T>> PipeCallable<Pulse, List<B>> flushOrConsumePackets(BoundedBuffer<T, B> inputBuffer){
        return flushInternal(inputBuffer, Flusher::flushOrConsume, input -> input);
    }

    static <T, B extends Packet<T>, C> PipeCallable<Pulse, List<C>> flushInternal(BoundedBuffer<T, B> inputBuffer, final PipeCallable<InputPort<T, B>, List<B>> flushingStrategy, final PipeCallable<B, C> unwrappingStrategy) {
        InputPort<T, B> inputPort = inputBuffer.createInputPort();

        return new PipeCallable<>() {
            @Override
            public List<C> call(Pulse input) {
                return flushingStrategy.call(inputPort).stream().map(unwrappingStrategy::call).collect(Collectors.toList());
            }

        };
    }

    private static <T, B extends Packet<T>> List<B> flush(InputPort<T, B> input) {
        try {
            return input.flush();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static <T, B extends Packet<T>> List<B> flushOrConsume(InputPort<T, B> input) {
        try {
            return input.flushOrConsume();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T, B extends Packet<T>> SimpleBuffer<List<T>, SimplePacket<List<T>>> flushToList(BoundedBuffer<T, B> inputBuffer) {
        SimpleBuffer<List<T>, SimplePacket<List<T>>> outputBuffer = new SimpleBuffer<>(1, "sound environment - raw audio out to byte list");
        new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {
            @Override
            protected void tick() {
                try {
                    List<B> flush = input.flush();
                    output.produce(new SimplePacket<>(flush.stream().map(Packet::unwrap).collect(Collectors.toList())));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        return outputBuffer;
    }
}
