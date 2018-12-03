package component;

import component.buffer.*;

public class Counter<A extends Packet<Pulse>, B extends Packet<Long>> extends MethodPipeComponent<Pulse, Long, A, B> {

    public Counter(SimpleBuffer<Pulse, A> inputBuffer, SimpleBuffer<Long, B> outputBuffer) {
        super(inputBuffer, outputBuffer, build());
    }

    public static PipeCallable<Pulse, Long> build() {
        return new PipeCallable<>() {
            private long calculatedTicks = 0L;

            @Override
            public Long call(Pulse input) {
                long oldCalculatedTicks = calculatedTicks;
                calculatedTicks++;
                return oldCalculatedTicks;
            }

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        };
    }
}
