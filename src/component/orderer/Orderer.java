package component.orderer;

import component.buffer.*;

import java.util.*;

public class Orderer<T> extends AbstractPipeComponent<LinkedList<LinkedList<OrderStampedPacket<T>>>, T, SimplePacket<LinkedList<LinkedList<OrderStampedPacket<T>>>>, OrderStampedPacket<T>> {
    private final PriorityQueue<LinkedList<OrderStampedPacket<T>>> backlog;
    private LinkedList<LinkedList<OrderStampedPacket<T>>> defragmentedBacklog;
    private OrderStampedPacket<T> index;

    public Orderer(BoundedBuffer<LinkedList<LinkedList<OrderStampedPacket<T>>>, SimplePacket<LinkedList<LinkedList<OrderStampedPacket<T>>>>> input, BoundedBuffer<T, OrderStampedPacket<T>> output) {
        super(input.createInputPort(), output.createOutputPort());
        backlog = new PriorityQueue<>(Comparator.comparing(o -> o.peek()));
        defragmentedBacklog = new LinkedList<>();
    }

    @Override
    protected void tick() {
        try {
            backlog.addAll(input.consume().unwrap());
            if (index != null) {
                clearBacklog();
                defragmentContinuously();
            } else {
                tryProduceFirstPacket();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void clearBacklog() throws InterruptedException {
        if(!backlog.isEmpty()) {
            defragmentBacklog();
        }
        if (!defragmentedBacklog.isEmpty() && index.successor(defragmentedBacklog.peek().peek())) {
            LinkedList<OrderStampedPacket<T>> nextFragment = defragmentedBacklog.poll();
            index = nextFragment.peekLast();
            for (OrderStampedPacket<T> packet : nextFragment) {
                output.produce(packet);
            }
        }
    }

    private void tryProduceFirstPacket() {
        if(!backlog.isEmpty() && backlog.peek().peek().hasFirstStamp()) {
            LinkedList<OrderStampedPacket<T>> topDefragmentedSegment = backlog.poll();
            OrderStampedPacket<T> firstPacket = topDefragmentedSegment.poll();
            index = firstPacket;
            try {
                output.produce(firstPacket);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!topDefragmentedSegment.isEmpty()){
                backlog.add(topDefragmentedSegment);
            }
        }
    }

    private void defragmentContinuously() {
        while(input.isEmpty() && !backlog.isEmpty()){
            defragmentBacklog();
        }
    }

    private void defragmentBacklog() {
        LinkedList<OrderStampedPacket<T>> fragmentToBePlaced = new LinkedList<>(backlog.poll());
        LinkedList<LinkedList<OrderStampedPacket<T>>> fragmentsLowerThanFragmentToBePlaced = new LinkedList<>();
        while(!defragmentedBacklog.isEmpty()){
            LinkedList<OrderStampedPacket<T>> otherFragment = defragmentedBacklog.poll();
            if(fragmentToBePlaced.peekLast().successor(otherFragment.peek())){
                fragmentToBePlaced.addAll(otherFragment);
                break;
            } else if(otherFragment.peekLast().successor(fragmentToBePlaced.peek())){
                otherFragment.addAll(fragmentToBePlaced);
                fragmentToBePlaced = otherFragment;
            } else if(fragmentToBePlaced.peekLast().compareTo(otherFragment.peek()) < 0){
                defragmentedBacklog.addFirst(otherFragment);
                break;
            } else {
                fragmentsLowerThanFragmentToBePlaced.add(otherFragment);
            }
        }
        defragmentedBacklog.addFirst(fragmentToBePlaced);
        fragmentsLowerThanFragmentToBePlaced.addAll(defragmentedBacklog);
        defragmentedBacklog = fragmentsLowerThanFragmentToBePlaced;
    }

    private static <T> LinkedList<LinkedList<OrderStampedPacket<T>>> defragment(PriorityQueue<OrderStampedPacket<T>> newPackets) {
        LinkedList<LinkedList<OrderStampedPacket<T>>> defragmentedInput = new LinkedList<>();
        LinkedList<OrderStampedPacket<T>> defragmentedSegment = new LinkedList<>(Collections.singletonList(newPackets.poll()));
        while(!newPackets.isEmpty()){
            OrderStampedPacket<T> candidate = newPackets.poll();
            if(defragmentedSegment.peekLast().successor(candidate)){
                defragmentedSegment.add(candidate);
            } else {
                defragmentedInput.add(defragmentedSegment);
                defragmentedSegment = new LinkedList<>(Collections.singletonList(candidate));
            }
        }
        defragmentedInput.add(defragmentedSegment);
        return defragmentedInput;
    }

    @Override
    public Boolean isParallelisable(){
        return false;
    }

    public static <T> PipeCallable<BoundedBuffer<T, OrderStampedPacket<T>>, BoundedBuffer<T, OrderStampedPacket<T>>> buildPipe(String name){
        return inputBuffer -> {
            SimpleBuffer<T, OrderStampedPacket<T>> outputBuffer = new SimpleBuffer<>(100, name);
            SimpleBuffer<LinkedList<LinkedList<OrderStampedPacket<T>>>, SimplePacket<LinkedList<LinkedList<OrderStampedPacket<T>>>>> defragmentedOrderedPackets = new SimpleBuffer<>(100, name + " defragmenter");
            new TickRunningStrategy(new AbstractPipeComponent<>(inputBuffer.resize(100).createInputPort(), defragmentedOrderedPackets.createOutputPort()) {
                @Override
                protected void tick() {
                    try {
                        PriorityQueue<OrderStampedPacket<T>> newPackets = new PriorityQueue<>(input.flushOrConsume());
                        output.produce(new SimplePacket<>(defragment(newPackets)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            new TickRunningStrategy(new Orderer<>(defragmentedOrderedPackets, outputBuffer));
            return outputBuffer;
        };
    }
}
