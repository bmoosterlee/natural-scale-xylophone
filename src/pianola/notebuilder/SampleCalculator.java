package pianola.notebuilder;

import component.Pairer;
import component.buffer.BoundedBuffer;
import component.buffer.Packet;
import component.buffer.PipeCallable;
import component.buffer.SimplePacket;
import frequency.Frequency;

import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

class SampleCalculator {
    static <I, K, O> BoundedBuffer<O, SimplePacket<O>> buildSampleCalculator(BoundedBuffer<I, SimplePacket<I>> inputBuffer, ConcurrentHashMap<Long, Set<AbstractMap.SimpleImmutableEntry<Frequency, K>>> unfinishedSampleFragments, PipeCallable<I, Long> addNewNotes, PipeCallable<AbstractMap.SimpleImmutableEntry<Long, AbstractMap.SimpleImmutableEntry<Frequency, K>>, O> calculation, BinaryOperator<O> add, Callable<O> outputIdentityBuilder, String name) {
        AbstractMap.SimpleImmutableEntry<BoundedBuffer<AbstractMap.SimpleImmutableEntry<Long, Set<AbstractMap.SimpleImmutableEntry<Frequency, K>>>, Packet<AbstractMap.SimpleImmutableEntry<Long, Set<AbstractMap.SimpleImmutableEntry<Frequency, K>>>>>, BoundedBuffer<Set<O>, Packet<Set<O>>>> precalculatorOutputs = inputBuffer
                .<Long, SimplePacket<Long>>performMethod(
                        addNewNotes,
                        100,
                        name + " - add new notes")
                .connectTo(MapPrecalculator.buildPipe(
                        unfinishedSampleFragments,
                        calculation,
                        name
                ));

        return Pairer.pair(
                precalculatorOutputs.getValue()
                        .performMethod(input1 -> {
                                    try {
                                        return input1.stream().reduce(outputIdentityBuilder.call(), add);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                },
                                100,
                                name + " - fold precalculated fragments"),
                precalculatorOutputs.getKey()
                        .performMethod(input2 -> {
                                    Long sampleCount = input2.getKey();
                                    return input2.getValue().stream().map(unfinishedFragment -> {
                                        try {
                                            return calculation.call(
                                                    new AbstractMap.SimpleImmutableEntry<>(
                                                            sampleCount,
                                                            unfinishedFragment));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            return null;
                                        }
                                    })
                                            .collect(Collectors.toSet());
                                },
                                100,
                                name + " - calculate fragments")
                        .performMethod(input2 -> {
                                    try {
                                        return input2.stream().reduce(outputIdentityBuilder.call(), add);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                },
                                100,
                                name + " - fold calculated fragments"),
                100,
                name + " - pair calculated and precalculated fragments")
                .performMethod(
                        input -> add.apply(input.getKey(), input.getValue()),
                        100,
                        name + " - construct finished sample");
    }
}
