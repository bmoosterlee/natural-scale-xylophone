package component.orderer;

import component.buffer.*;

public class OrderStamper {
    private OrderStamp counter;
    private OrderStamp firstStamp;

    private OrderStamper(){
        firstStamp = new OrderStamp(0);
        counter = firstStamp;
    }

    public static <T, A extends Packet<T>> PipeCallable<BoundedBuffer<T, A>, BoundedBuffer<T, OrderStampedPacket<T>>> buildPipe(int capacity){
        return inputBuffer -> {
            SimpleBuffer<T, OrderStampedPacket<T>> outputBuffer = new SimpleBuffer<>(capacity, "order stamper");

            InputPort<T, A> inputPort = inputBuffer.createInputPort();
            new TickRunningStrategy(new AbstractPipeComponent<>(inputPort, outputBuffer.createOutputPort()) {
                OrderStamper orderStamper = new OrderStamper();

                @Override
                protected void tick() {
                    try {
                        output.produce(
                                new OrderStampedPacket<>(
                                        orderStamper,
                                        orderStamper.stamp(),
                                        input.consume().unwrap()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            });

            return outputBuffer;
        };
    }

    OrderStamp stamp() {
        OrderStamp orderStamp = counter;
        counter = counter.successor();
        return orderStamp;
    }

    public boolean firstStamp(OrderStampedPacket packet) {
        return packet.stamp == firstStamp;
    }
}
