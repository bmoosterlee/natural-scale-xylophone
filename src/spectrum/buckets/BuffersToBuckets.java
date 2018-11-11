package spectrum.buckets;

import component.*;

import java.util.*;

public class BuffersToBuckets extends TickablePipeComponent<Pulse, Buckets> {

    public BuffersToBuckets(BufferInterface<Pulse> tickBuffer, Map<Integer, BufferInterface<AtomicBucket>> inputMap, BoundedBuffer<Buckets> outputBuffer) {
        super(tickBuffer, outputBuffer, toBuckets(inputMap));
    }

    public static CallableWithArguments<Pulse, Buckets> toBuckets(Map<Integer, BufferInterface<AtomicBucket>> bufferMap) {
        return new CallableWithArguments<>() {
            private OutputPort<Pulse> methodInputPort;
            private InputPort<Buckets> methodOutputPort;

            {
                BufferInterface<Pulse> methodInput = new BoundedBuffer<>(1, "BuffersToBuckets - input");
                methodInputPort = methodInput.createOutputPort();

                LinkedList<BufferInterface<Pulse>> frameTickBroadcast = new LinkedList<>(methodInput.broadcast(bufferMap.size()));
                Map<Integer, BufferInterface<Pulse>> frameTickers = new HashMap<>();
                for (Integer index : bufferMap.keySet()) {
                    frameTickers.put(index, frameTickBroadcast.poll());
                }

                Map<Integer, CallableWithArguments<Pulse, List<AtomicBucket>>> flushers = new HashMap<>();
                for (Integer index : bufferMap.keySet()) {
                    flushers.put(index, Flusher.flush(bufferMap.get(index)));
                }

                methodOutputPort =
                    collect(
                        forEach(
                            forEach(frameTickers, flushers),
                                input1 -> new MemoizedBucket(new CompositeBucket<>(input1))))
                    .performMethod(Buckets::new).createInputPort();
            }

            @Override
            public Buckets call(Pulse input) {
                try {
                    methodInputPort.produce(input);

                    Buckets result = methodOutputPort.consume();

                    return result;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    public static <I, K, V> Map<I, BufferInterface<V>> forEach(Map<I, BufferInterface<K>> input, Map<I, CallableWithArguments<K, V>> methods) {
        Map<I, BufferInterface<V>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(methods.get(index)));
        }
        return output;
    }

    public static <I, K, V> Map<I, BufferInterface<V>> forEach(Map<I, BufferInterface<K>> input, CallableWithArguments<K, V> method) {
        Map<I, BufferInterface<V>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(method));
        }
        return output;
    }

    public static <I, K> BoundedBuffer<Map<I, K>> collect(Map<I, BufferInterface<K>> input){
        try {
            Map<I, K> map = new HashMap<>();
            for (I index : input.keySet()) {
                map.put(index, input.get(index).createInputPort().consume());
            }
            BufferInterface<Map<I,K>> output = new BoundedBuffer<>(1, "collect");
            output.createOutputPort().produce(map);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
        return null;
    }
}
