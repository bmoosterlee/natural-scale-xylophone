package sound;

import component.Counter;
import component.Pulse;
import component.buffer.*;
import time.Pulser;
import time.TimeInSeconds;

public class SampleTicker {
    public static BoundedBuffer<Long, SimplePacket<Long>> buildComponent(SampleRate sampleRate) {
        SimpleBuffer<Pulse, SimplePacket<Pulse>> sampleTickerBuffer = new SimpleBuffer<>(new OverflowStrategy<>("sample ticker overflow"));
        BoundedBuffer<Long, SimplePacket<Long>> stampedSamplesBuffer = sampleTickerBuffer
                .performMethod(Counter.build(), sampleRate.sampleRate / 32, "count samples");
        new TickRunningStrategy(new Pulser(sampleTickerBuffer, new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate)));
        return stampedSamplesBuffer;
    }
}
