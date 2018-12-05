package component.orderer;

import component.buffer.*;

import java.util.*;

public class Orderer<T> extends AbstractPipeComponent<T, T, OrderStampedPacket<T>, OrderStampedPacket<T>> {
    private final PriorityQueue<LinkedList<OrderStampedPacket<T>>> backlog;
    private final PriorityQueue<LinkedList<OrderStampedPacket<T>>> defragmentedBacklog;
    private OrderStampedPacket<T> index;

    public Orderer(BoundedBuffer<T, OrderStampedPacket<T>> input, BoundedBuffer<T, OrderStampedPacket<T>> output) {
        super(input.createInputPort(), output.createOutputPort());
        backlog = new PriorityQueue<>(Comparator.comparing(o -> o.peek()));
        defragmentedBacklog = new PriorityQueue<>(Comparator.comparing(LinkedList::peek));
    }

    @Override
    protected void tick() {
        try {
            PriorityQueue<OrderStampedPacket<T>> newPackets = new PriorityQueue<>(input.flushOrConsume());
            LinkedList<LinkedList<OrderStampedPacket<T>>> defragmentedInput = defragment(newPackets);
            
            backlog.addAll(defragmentedInput);

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

    private LinkedList<LinkedList<OrderStampedPacket<T>>> defragment(PriorityQueue<OrderStampedPacket<T>> newPackets) {
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

    private void clearBacklog() throws InterruptedException {
        while ((!defragmentedBacklog.isEmpty() && index.successor(defragmentedBacklog.peek().peek())) || (!backlog.isEmpty() && index.successor(backlog.peek().peek()))) {
            if (!defragmentedBacklog.isEmpty() && index.successor(defragmentedBacklog.peek().peek())) {
                LinkedList<OrderStampedPacket<T>> nextFragment = defragmentedBacklog.poll();
                index = nextFragment.peekLast();
                for (OrderStampedPacket<T> packet : nextFragment) {
                    output.produce(packet);
                }
            }
            if (!backlog.isEmpty() && index.successor(backlog.peek().peek())) {
                LinkedList<OrderStampedPacket<T>> topDefragmentedSegment = backlog.poll();
                index = topDefragmentedSegment.peekLast();
                while(!topDefragmentedSegment.isEmpty()) {
                    output.produce(topDefragmentedSegment.poll());
                }
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
            LinkedList<OrderStampedPacket<T>> newEntry = backlog.poll();
            HashSet<LinkedList<OrderStampedPacket<T>>> candidatesLowerThanEntry = new HashSet<>();
            while(!defragmentedBacklog.isEmpty()){
                LinkedList<OrderStampedPacket<T>> candidate = defragmentedBacklog.poll();
                if(newEntry.peekLast().successor(candidate.peek())){
                    newEntry.addAll(candidate);
                    break;
                } else if(candidate.peekLast().successor(newEntry.peek())){
                    candidate.addAll(newEntry);
                    newEntry = candidate;
                } else if(newEntry.peekLast().compareTo(candidate.peek()) < 0){
                    defragmentedBacklog.add(candidate);
                    break;
                } else {
                    candidatesLowerThanEntry.add(candidate);
                }
            }
            defragmentedBacklog.addAll(candidatesLowerThanEntry);
            defragmentedBacklog.add(newEntry);
        }
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
