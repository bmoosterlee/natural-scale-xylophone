package component;

import component.buffer.*;

import java.util.Collection;
import java.util.HashSet;

public class Separator{

    public static <T, L extends Collection<T>, A extends Packet<T>, B extends Packet<L>> PipeCallable<BoundedBuffer<L, B>, BoundedBuffer<T, A>> buildPipe(){
        return separateInternal(Separator::separate);
    }

    static <T, L extends Collection<A>, A extends Packet<T>, B extends Packet<L>> PipeCallable<BoundedBuffer<L, B>, BoundedBuffer<T, A>> buildPacketsPipe(){
        return separateInternal(Separator::separatePackets);
    }

    private static <T, L extends Collection<?>, A extends Packet<T>, B extends Packet<L>> PipeCallable<BoundedBuffer<L, B>, BoundedBuffer<T, A>> separateInternal(final PipeCallable<B, Collection<A>> separationStrategy) {
        return inputBuffer -> {
            SimpleBuffer<T, A> outputBuffer = new SimpleBuffer<>(1, "separator - output");
            new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.createInputPort(), outputBuffer.createOutputPort()) {
                @Override
                protected void tick() {
                    try {
                        B consumed = input.consume();

                        Collection<A> col = separationStrategy.call(consumed);

                        for (A element : col) {
                            output.produce(element);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public Boolean isParallelisable(){
                    return false;
                }

            });
            return outputBuffer;
        };
    }

    private static <T, L extends Collection<T>, A extends Packet<T>, B extends Packet<L>> Collection<A> separate(B consumed) {
        Collection<A> col = new HashSet<>();
        for (T element : consumed.unwrap()) {
            col.add(consumed.transform(in -> element));
        }
        return col;
    }

    private static <T, L extends Collection<A>, A extends Packet<T>, B extends Packet<L>> Collection<A> separatePackets(B consumed) {
        return new HashSet<>(consumed.unwrap());
    }
}
