package sound;

import component.Counter;
import component.Pulse;
import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.OrderStamper;
import time.Pulser;
import time.TimeInSeconds;

public class SampleTicker {
    public static BoundedBuffer<Long, OrderStampedPacket<Long>> buildComponent(SampleRate sampleRate) {
        SimpleBuffer<Pulse, SimplePacket<Pulse>> sampleTickerBuffer = new SimpleBuffer<>(new OverflowStrategy<>("main - sample ticker overflow"));
        BoundedBuffer<Long, OrderStampedPacket<Long>> stampedSamplesBuffer = sampleTickerBuffer
                .performMethod(Counter.build(), sampleRate.sampleRate / 32, "mixer - count samples")
                .connectTo(OrderStamper.buildPipe(sampleRate.sampleRate / 32));
        new TickRunningStrategy(new Pulser(sampleTickerBuffer, new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate)));
        return stampedSamplesBuffer;
    }
}
