package component.buffer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TrafficAnalyzer {

    protected static TrafficAnalyzer trafficAnalyzer;

    private Map<String, AtomicInteger> clogLog;
    public Map<String, AtomicInteger> topClogs;
    private Map<String, Collection<String>> components;
    private Set<String> rootComponents;
    private Map<String, BoundedBuffer> bufferMap;
    private Map<String, Callable<Integer>> threadCountMap;
    private Callable<Void> print;

    public TrafficAnalyzer(){
        clogLog = new ConcurrentHashMap<>();
        components = new HashMap<>();
        rootComponents = new HashSet<>();
        bufferMap = new HashMap<>();
        threadCountMap = new HashMap<>();
        trafficAnalyzer = this;

        print = new Callable<>() {
            Map<String, String> passedBuffers = new HashMap<>();

            @Override
            public Void call() {
                passedBuffers.clear();
                for(String root : rootComponents){
                    print("", root);
                    System.out.println("END OF SUBGRAPH");
                    System.out.println();
                }
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
                String threadCount;
                Integer threadCountValue = null;
                if(threadCountMap.containsKey(node)) {
                    try {
                        threadCountValue = threadCountMap.get(node).call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    threadCountValue = 0;
                }

                threadCount = String.valueOf(threadCountValue);


                System.out.println(cloggingValue + "    " + threadCount + " " + parentChain + node);

                if(components.containsKey(node)){
                    Collection<String> outputs = components.get(node);
                    int i = 0;
                    for (String output : outputs) {
                        if(i!=outputs.size()-1) {
                            if(passedBuffers.containsKey(output)){
                                System.out.println("    " + "    " + "   " + parentChain + "|---\"" + output + "\" is continued at node \"" + passedBuffers.get(output) + "\"");
                            } else {
                                passedBuffers.put(output, node);
                                print(parentChain + "|---", output);
                            }
                        } else {
                            if(passedBuffers.containsKey(output)){
                                System.out.println("    " + "    " + "   " + parentChain + "    \"" + output + "\" is continued at node \"" + passedBuffers.get(output) + "\"");
                            } else {
                                passedBuffers.put(output, node);
                                print(parentChain + "    ", output);
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
                    Thread.sleep(10000);
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
        return priorityQueue.stream().sorted(entryComparator.reversed()).collect(Collectors.toMap(input -> input.getKey(), input -> input.getValue(), (e1, e2) -> e1, LinkedHashMap::new));
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

    public void addComponent(List<String> inputNames, List<String> outputNames, Map<String, BoundedBuffer> bufferMap, Callable<Integer> threadCountFunction) {
        this.bufferMap.putAll(bufferMap);

        if(!inputNames.isEmpty()) {
            Set<String> nonRootNodes = components.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            LinkedList<String> rootInputNames = new LinkedList<>(inputNames);
            rootInputNames.removeAll(nonRootNodes);
            rootComponents.addAll(rootInputNames);
            rootComponents.removeAll(outputNames);
        } else {
            rootComponents.addAll(outputNames);
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

        inputNames.forEach(inputName -> {
            if (!threadCountMap.containsKey(inputName)) {
                threadCountMap.put(inputName, threadCountFunction);
            } else {
                Callable<Integer> oldCallable = threadCountMap.get(inputName);
                threadCountMap.put(inputName, () -> oldCallable.call() + threadCountFunction.call());
            }
        });
    }
}
