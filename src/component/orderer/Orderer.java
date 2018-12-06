package component.orderer;

import component.Separator;
import component.buffer.*;

import java.util.*;

public class Orderer<T> extends AbstractPipeComponent<T, T, OrderStampedPacket<T>, OrderStampedPacket<T>> {
    private final PriorityQueue<OrderStampedPacket<T>> backlog;
    private LinkedList<LinkedList<OrderStampedPacket<T>>> defragmentedBacklog;
    private OrderStampedPacket<T> index;

    public Orderer(BoundedBuffer<T, OrderStampedPacket<T>> input, BoundedBuffer<T, OrderStampedPacket<T>> output) {
        super(input.createInputPort(), output.createOutputPort());
        backlog = new PriorityQueue<>();
        defragmentedBacklog = new LinkedList<>();
    }

    @Override
    protected void tick() {
        try {
            backlog.add(input.consume());
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

    private void clearBacklog() {
        if(!backlog.isEmpty()) {
            defragmentBacklog();
        }
        if (!defragmentedBacklog.isEmpty() && index.successor(defragmentedBacklog.peek().peek())) {
            LinkedList<OrderStampedPacket<T>> nextFragment = defragmentedBacklog.poll();
            index = nextFragment.peekLast();
            nextFragment.forEach(input ->
            {
                try {
                    output.produce(input);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            );
        }
    }

    private void tryProduceFirstPacket() {
        if(!backlog.isEmpty() && backlog.peek().hasFirstStamp()) {
            OrderStampedPacket<T> firstPacket = backlog.poll();
            index = firstPacket;
            try {
                output.produce(firstPacket);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
        return inputBuffer -> {
            SimpleBuffer<T, OrderStampedPacket<T>> outputBuffer = new SimpleBuffer<>(1, name);
            new TickRunningStrategy(new Orderer<>(inputBuffer, outputBuffer));
            return outputBuffer;
        };
    }
}
