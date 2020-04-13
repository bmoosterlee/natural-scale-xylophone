package component;

import component.buffer.*;

import java.util.LinkedList;
import java.util.List;

public class Batcher {

    public static <K, V, A extends Packet<K>, B extends Packet<V>> PipeCallable<BoundedBuffer<K, A>, BoundedBuffer<V, B>> buildPipe(PipeCallable<K, V> method){
        return inputBuffer -> {
            SimpleBuffer<Pulse, SimplePacket<Pulse>> batchPulses = new SimpleBuffer<>(inputBuffer.getSize(), "batcher - pulse to flusher");

            LinkedList<SimpleBuffer<List<B>, SimplePacket<List<B>>>> processedBatch = new LinkedList<>(
                    batchPulses
                    .performMethodUnchained(Flusher.flushOrConsumePackets(inputBuffer), "batcher - input")
                    .<List<B>, SimplePacket<List<B>>>performMethodUnchained(batch(input1 -> input1.transform(method)), "batcher - processed batch")
                    .broadcast(2, "batcher - processed batch broadcast"));

            processedBatch.poll()
                    .performMethodUnchained(input -> new Pulse(), "batcher - output to pulse")
                    .relayTo(batchPulses);

            try {
                batchPulses.createOutputPort().produce(new SimplePacket<>(new Pulse()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return processedBatch.poll().connectTo(Separator.buildPacketsPipe());
        };
    }

    private static <K, V> PipeCallable<List<K>, List<V>> batch(PipeCallable<K, V> method) {
        return input -> {
            List<V> results = new LinkedList<>();
            for (K element : input) {
                results.add(method.call(element));
            }
            return results;
        };
    }
}
