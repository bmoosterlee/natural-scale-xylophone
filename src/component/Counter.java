package component;

import component.buffer.*;

public class Counter {

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
