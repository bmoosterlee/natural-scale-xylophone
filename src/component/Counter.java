package component;

import component.buffer.*;

public class Counter extends MethodPipeComponent<Pulse, Long> {

    public Counter(SimpleBuffer<Pulse> inputBuffer, SimpleBuffer<Long> outputBuffer) {
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
        };
    }
}
