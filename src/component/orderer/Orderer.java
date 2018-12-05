package component.orderer;

import component.buffer.*;

import java.util.*;

public class Orderer<T> extends AbstractPipeComponent<T, T, OrderStampedPacket<T>, OrderStampedPacket<T>> {
    private final PriorityQueue<OrderStampedPacket<T>> backlog;
    private final PriorityQueue<LinkedList<OrderStampedPacket<T>>> defragmentedBacklog;
    private OrderStampedPacket<T> index;

    public Orderer(BoundedBuffer<T, OrderStampedPacket<T>> input, BoundedBuffer<T, OrderStampedPacket<T>> output) {
        super(input.createInputPort(), output.createOutputPort());
        backlog = new PriorityQueue<>();
        defragmentedBacklog = new PriorityQueue<>(Comparator.comparing(LinkedList::peek));
    }

    @Override
    protected void tick() {
        try {
            List<OrderStampedPacket<T>> newPackets = input.flushOrConsume();

            backlog.addAll(newPackets);
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
        while ((!defragmentedBacklog.isEmpty() && index.successor(defragmentedBacklog.peek().peek())) || (!backlog.isEmpty() && index.successor(backlog.peek()))) {
            if (!defragmentedBacklog.isEmpty() && index.successor(defragmentedBacklog.peek().peek())) {
                LinkedList<OrderStampedPacket<T>> nextFragment = defragmentedBacklog.poll();
                index = nextFragment.peekLast();
                for (OrderStampedPacket<T> packet : nextFragment) {
                    output.produce(packet);
                }
            }
            if (!backlog.isEmpty() && index.successor(backlog.peek())) {
                index = backlog.poll();
                output.produce(index);
            }
        }
    }

    private void tryProduceFirstPacket() {
        if(backlog.peek().hasFirstStamp()) {
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
            LinkedList<OrderStampedPacket<T>> newEntry = new LinkedList<>(Collections.singletonList(backlog.poll()));
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
