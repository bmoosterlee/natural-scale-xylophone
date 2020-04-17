package component;

import component.buffer.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Separator{

    public static <T, L extends Collection<T>, A extends Packet<T>, B extends Packet<L>> PipeCallable<BoundedBuffer<L, B>, BoundedBuffer<T, A>> buildPipe(String name){
        return separateInternal(Separator::separate, name);
    }

    public static <T, L extends Collection<A>, A extends Packet<T>, B extends Packet<L>> PipeCallable<BoundedBuffer<L, B>, BoundedBuffer<T, A>> buildPacketsPipe(String name){
        return separateInternal(Separator::separatePackets, name);
    }

    private static <T, L extends Collection<?>, A extends Packet<T>, B extends Packet<L>> PipeCallable<BoundedBuffer<L, B>, BoundedBuffer<T, A>> separateInternal(final PipeCallable<B, List<A>> separationStrategy, String name) {
        return inputBuffer -> {
            SimpleBuffer<T, A> outputBuffer = new SimpleBuffer<>(1, name);
            new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {
                @Override
                protected void tick() {
                    try {
                        B consumed = input.consume();

                        List<A> col = separationStrategy.call(consumed);

                        for (A element : col) {
                            output.produce(element);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            });
            return outputBuffer;
        };
    }

    private static <T, L extends Collection<T>, A extends Packet<T>, B extends Packet<L>> List<A> separate(B consumed) {
        List<A> col = new LinkedList<>();
        for (T element : consumed.unwrap()) {
            col.add(consumed.transform(in -> element));
        }
        return col;
    }

    private static <T, L extends Collection<A>, A extends Packet<T>, B extends Packet<L>> List<A> separatePackets(B consumed) {
        return new LinkedList<>(consumed.unwrap());
    }
}
