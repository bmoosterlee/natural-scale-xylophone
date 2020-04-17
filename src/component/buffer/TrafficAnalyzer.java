package component.buffer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TrafficAnalyzer {

    static TrafficAnalyzer trafficAnalyzer;
    private Map<String, AtomicInteger> clogLog;
    private final ConcurrentHashMap<String, AtomicInteger> throughputLog;
    Map<String, AtomicInteger> topClogs;
    private Map<String, Collection<String>> components;
    private Set<String> rootComponents;
    private Map<String, BoundedBuffer> bufferMap;
    private Callable<Void> print;

    public TrafficAnalyzer(){
        clogLog = new ConcurrentHashMap<>();
        throughputLog = new ConcurrentHashMap<>();
        components = new HashMap<>();
        rootComponents = new HashSet<>();
        bufferMap = new HashMap<>();
        trafficAnalyzer = this;

        print = new Callable<>() {
            Map<String, String> buffersAlreadyPrintedAt = new HashMap<>();

            @Override
            public Void call() {
                buffersAlreadyPrintedAt.clear();
                System.out.println();
                System.out.println("START OF TRAFFIC ANALYSIS");
                synchronized (rootComponents) {
                    for (String root : rootComponents) {
                        print("", root);
                        System.out.println("END OF SUBGRAPH");
                        System.out.println();
                    }
                }
                System.out.println("END OF TRAFFIC ANALYSIS");
                System.out.println();
                return null;
            }

            private void print(String parentChain, String node) {
                Map<String, AtomicInteger> currenTopClogs = topClogs;

                String cloggingValue;
                if(currenTopClogs.containsKey(node)){
                    cloggingValue = String.valueOf(currenTopClogs.get(node));
                } else {
                    cloggingValue = "   ";
                }

                String throughputValue;
                if(throughputLog.containsKey(node)){
                    throughputValue = String.valueOf(throughputLog.remove(node));
                } else {
                    throughputValue = "   ";
                }

                System.out.println(cloggingValue + "    " + throughputValue + "    " + parentChain + node);

                if(components.containsKey(node)){
                    Collection<String> outputBufferNames = components.get(node);
                    int i = 0;
                    for (String outputBufferName : outputBufferNames) {
                        if(i!=outputBufferNames.size()-1) {
                            if(buffersAlreadyPrintedAt.containsKey(outputBufferName)){
                                System.out.println("    " + "    " + "   " + parentChain + "|---\"" + outputBufferName + "\" is continued at node \"" + buffersAlreadyPrintedAt.get(outputBufferName) + "\"");
                            } else {
                                buffersAlreadyPrintedAt.put(outputBufferName, node);
                                print(parentChain + "|---", outputBufferName);
                            }
                        } else {
                            if(buffersAlreadyPrintedAt.containsKey(outputBufferName)){
                                System.out.println("    " + "    " + "   " + parentChain + "    \"" + outputBufferName + "\" is continued at node \"" + buffersAlreadyPrintedAt.get(outputBufferName) + "\"");
                            } else {
                                buffersAlreadyPrintedAt.put(outputBufferName, node);
                                print(parentChain + "    ", outputBufferName);
                            }
                        }
                        i++;
                    }
                }
            }
        };
        start();
    }

    private void start() {
        new Thread(() -> {
            while(true){
                topClogs = getTopClogs();
                try {
                    print.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private LinkedHashMap<String, AtomicInteger> getTopClogs(){
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
        return priorityQueue.stream().sorted(entryComparator.reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private void logClogInternal(String bufferName){
        if(!components.containsKey(bufferName))
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

    public static void logConsumption(String bufferName){ trafficAnalyzer.logConsumptionInternal(bufferName);}

    private void logConsumptionInternal(String bufferName) {
        Map<String, AtomicInteger> throughputLog = this.throughputLog;
        if(throughputLog.containsKey(bufferName)) {
            try {
                throughputLog.get(bufferName).incrementAndGet();
            } catch(NullPointerException ignored){

            }
        } else {
            throughputLog.put(bufferName, new AtomicInteger(1));
        }
    }

    public void addComponent(List<String> inputNames, List<String> outputNames, Map<String, BoundedBuffer> bufferMap) {
        this.bufferMap.putAll(bufferMap);

        synchronized (rootComponents) {
            if (!inputNames.isEmpty()) {
                Set<String> nonRootNodes = components.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
                LinkedList<String> rootInputNames = new LinkedList<>(inputNames);
                rootInputNames.removeAll(nonRootNodes);
                rootComponents.addAll(rootInputNames);
                rootComponents.removeAll(outputNames);

            } else {
                rootComponents.addAll(outputNames);
            }
        }

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
