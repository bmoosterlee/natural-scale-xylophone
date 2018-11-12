package spectrum.buckets;

import component.*;
import component.buffer.*;
import component.utilities.RunningOutputComponent;
import component.utilities.RunningPipeComponent;

import java.util.*;

public class BuffersToBuckets extends RunningPipeComponent<Pulse, Buckets> {

    public BuffersToBuckets(BoundedBuffer<Pulse> tickBuffer, Map<Integer, BoundedBuffer<AtomicBucket>> inputMap, SimpleBuffer<Buckets> outputBuffer) {
        super(tickBuffer, outputBuffer, toBuckets(inputMap));
    }

    public static CallableWithArguments<Pulse, Buckets> toBuckets(Map<Integer, BoundedBuffer<AtomicBucket>> bufferMap) {
        return new CallableWithArguments<>() {
            private OutputPort<Pulse> methodInputPort;
            private InputPort<Buckets> methodOutputPort;

            {
                BoundedBuffer<Pulse> methodInput = new SimpleBuffer<>(1, "BuffersToBuckets - input");
                methodInputPort = methodInput.createOutputPort();

                LinkedList<BoundedBuffer<Pulse>> frameTickBroadcast = new LinkedList<>(methodInput.broadcast(bufferMap.size()));
                Map<Integer, BoundedBuffer<Pulse>> frameTickers = new HashMap<>();
                for (Integer index : bufferMap.keySet()) {
                    frameTickers.put(index, frameTickBroadcast.poll());
                }

                Map<Integer, CallableWithArguments<Pulse, List<AtomicBucket>>> flushers = new HashMap<>();
                for (Integer index : bufferMap.keySet()) {
                    flushers.put(index, Flusher.flush(bufferMap.get(index)));
                }

                methodOutputPort =
                    collect(
                        toInputPorts(
                            forEach(
                                forEach(frameTickers, flushers),
                                input1 -> new MemoizedBucket(new CompositeBucket<>(input1)))))
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

    public static <I, K, V> Map<I, BoundedBuffer<V>> forEach(Map<I, BoundedBuffer<K>> input, Map<I, CallableWithArguments<K, V>> methods) {
        Map<I, BoundedBuffer<V>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(methods.get(index)));
        }
        return output;
    }

    public static <I, K, V> Map<I, BoundedBuffer<V>> forEach(Map<I, BoundedBuffer<K>> input, CallableWithArguments<K, V> method) {
        Map<I, BoundedBuffer<V>> output = new HashMap<>();
        for (I index : input.keySet()) {
            output.put(index, input.get(index).performMethod(method));
        }
        return output;
    }

    public static <I, K> Map<I, InputPort<K>> toInputPorts(Map<I, BoundedBuffer<K>> input){
        Map<I, InputPort<K>> map = new HashMap<>();
        for (I index : input.keySet()) {
            map.put(index, input.get(index).createInputPort());
        }
        return map;
    }

    public static <I, K> SimpleBuffer<Map<I, K>> collect(Map<I, InputPort<K>> input){
        return RunningOutputComponent.buildOutputBuffer(
            () -> {
                try {
                    Map<I, K> map = new HashMap<>();
                    for (I index : input.keySet()) {
                        map.put(index, input.get(index).consume());
                    }

                    return map;
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
                return null;
            },
        1,
        "buffers to buckets - collect");
    }
}
