package component;

public class Counter extends TickablePipeComponent<Pulse, Long> {

    public Counter(BoundedBuffer<Pulse> inputBuffer, SimpleBuffer<Long> outputBuffer) {
        super(inputBuffer, outputBuffer, build());
    }

    public static CallableWithArguments<Pulse, Long> build() {
        return new CallableWithArguments<>() {
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
