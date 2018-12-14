package component.orderer;

import component.buffer.*;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class Orderer<T> extends AbstractPipeComponent<T, T, OrderStampedPacket<T>, OrderStampedPacket<T>> {
    private final PriorityBlockingQueue<OrderStampedPacket<T>> backlog;
    private LinkedList<LinkedList<OrderStampedPacket<T>>> defragmentedBacklog;
    private OrderStampedPacket<T> index;

    public Orderer(BoundedBuffer<T, OrderStampedPacket<T>> input, BoundedBuffer<T, OrderStampedPacket<T>> output) {
        super(input.createInputPort(), output.createOutputPort());
        backlog = new PriorityBlockingQueue<>();
        defragmentedBacklog = new LinkedList<>();
    }

    @Override
    protected void tick() {
        try {
            OrderStampedPacket<T> consumed = input.consume();
            if ((index != null && index.successor(consumed)) || (index == null && consumed.hasFirstStamp())) {
                output.produce(consumed);
                index = clearBacklog(consumed);
            } else{
                backlog.add(consumed);
            }
//            defragmentContinuously();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private OrderStampedPacket<T> clearBacklog(OrderStampedPacket<T> startingIndex) throws InterruptedException {
        OrderStampedPacket<T> currentIndex = startingIndex;
        while((!backlog.isEmpty() && currentIndex.successor(backlog.peek()) || (!defragmentedBacklog.isEmpty() && currentIndex.successor(defragmentedBacklog.peek().peek())))) {
            if(!backlog.isEmpty() && currentIndex.successor(backlog.peek())) {
                OrderStampedPacket<T> nextPacket = backlog.poll();
                currentIndex = nextPacket;
                output.produce(nextPacket);
            }
            if (!defragmentedBacklog.isEmpty() && currentIndex.successor(defragmentedBacklog.peek().peek())) {
                LinkedList<OrderStampedPacket<T>> nextFragment = defragmentedBacklog.poll();
                currentIndex = nextFragment.peekLast();
                for (OrderStampedPacket<T> tOrderStampedPacket : nextFragment) {
                    output.produce(tOrderStampedPacket);
                }
            }
        }
        return currentIndex;
    }

    private void defragmentContinuously() {
        while(input.isEmpty() && !backlog.isEmpty()){
            defragmentBacklog();
        }
    }

    private void defragmentBacklog() {
        LinkedList<OrderStampedPacket<T>> fragmentToBePlaced = new LinkedList<>(Collections.singletonList(backlog.poll()));
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
        return buildPipe(1, name);
    }

    public static <T> PipeCallable<BoundedBuffer<T, OrderStampedPacket<T>>, BoundedBuffer<T, OrderStampedPacket<T>>> buildPipe(int capacity, String name){
        return inputBuffer -> {
            SimpleBuffer<T, OrderStampedPacket<T>> outputBuffer = new SimpleBuffer<>(capacity, name);
            new TickRunningStrategy(new Orderer<>(inputBuffer, outputBuffer));
            return outputBuffer;
        };
    }
}
