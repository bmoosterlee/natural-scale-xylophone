package component.buffer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrafficAnalyzer {

    protected static TrafficAnalyzer trafficAnalyzer;

    private Map<String, AtomicInteger> clogLog;
    public List<Map.Entry<String, AtomicInteger>> topClogs;
    private Map<String, Collection<String>> components;
    private Set<String> rootComponents;
    private Map<String, BoundedBuffer> bufferMap;

    public TrafficAnalyzer(){
        clogLog = new ConcurrentHashMap<>();
        components = new HashMap<>();
        rootComponents = new HashSet<>();
        bufferMap = new HashMap<>();
        trafficAnalyzer = this;
        start();
    }

    private void start() {
        new Thread(() -> {
            while(true){
                topClogs = getTopClogs();
                for (Map.Entry<String, AtomicInteger> clogEntry : topClogs) {
                    System.out.println(String.valueOf(clogEntry.getValue()) + ": " + clogEntry.getKey());
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private List<Map.Entry<String, AtomicInteger>> getTopClogs(){
        Map<String, AtomicInteger> currentClogLog = clogLog;
        clogLog = new ConcurrentHashMap<>();
        Comparator<Map.Entry<String, AtomicInteger>> entryComparator = Comparator.comparingInt(o -> o.getValue().get());
        PriorityQueue<Map.Entry<String, AtomicInteger>> priorityQueue = new PriorityQueue<>(entryComparator);
        for (Map.Entry<String, AtomicInteger> entry : currentClogLog.entrySet()) {
            priorityQueue.add(entry);
            if(priorityQueue.size() > 10){
                priorityQueue.poll();
            }
        }
        ArrayList<Map.Entry<String, AtomicInteger>> topResults = new ArrayList<>(priorityQueue);
        topResults.sort(entryComparator.reversed());
        return topResults;
    }

    private void logClogInternal(String bufferName){
        Collection<String> outputNames = components.get(bufferName);
        if(outputNames==null || !TickRunnerSpawner.anyClog(outputNames.stream().map(bufferMap::get).collect(Collectors.toList())))
        {
            Map<String, AtomicInteger> clogLog = this.clogLog;
            if (clogLog.containsKey(bufferName)) {
                clogLog.get(bufferName).incrementAndGet();
            } else {
                clogLog.put(bufferName, new AtomicInteger(1));
            }
        }
    }

    public static void logClog(String bufferName){
        trafficAnalyzer.logClogInternal(bufferName);
    }

    public void addComponent(List<String> inputNames, List<String> outputNames, Map<String, BoundedBuffer> bufferMap) {
        this.bufferMap.putAll(bufferMap);

        Set<String> nonRootNodes = components.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        LinkedList<String> rootInputNames = new LinkedList<>(inputNames);
        rootInputNames.removeAll(nonRootNodes);
        rootComponents.addAll(rootInputNames);

        rootComponents.removeAll(outputNames);

        if(!outputNames.isEmpty()) {
            inputNames.forEach(inputName -> {
                if (!components.containsKey(inputName)) {
                    components.put(inputName, outputNames);
                } else {
                    LinkedList<String> value = new LinkedList<>(components.get(inputName));
                    value.addAll(outputNames);
                    components.put(inputName, value);
                }
            });
        }
    }
}
