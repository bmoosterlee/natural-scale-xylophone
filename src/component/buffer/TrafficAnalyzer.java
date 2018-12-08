package component.buffer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TrafficAnalyzer {

    protected static TrafficAnalyzer trafficAnalyzer;

    private Map<String, AtomicInteger> clogLog;
    public List<Map.Entry<String, AtomicInteger>> topClogs;

    public TrafficAnalyzer(){
        clogLog = new ConcurrentHashMap<>();
        trafficAnalyzer = this;
        start();
    }

    private void start() {
        new Thread(() -> {
            while(true){
                topClogs = getTopClogs();
                for (Map.Entry<String, AtomicInteger> clogEntry : topClogs.subList(0,Math.min(10, topClogs.size()))) {
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
            if(priorityQueue.size() > 30){
                priorityQueue.poll();
            }
        }
        ArrayList<Map.Entry<String, AtomicInteger>> topResults = new ArrayList<>(priorityQueue);
        topResults.sort(entryComparator.reversed());
        return topResults;
    }

    private void logClogInternal(String bufferName){
        Map<String, AtomicInteger> clogLog = this.clogLog;
        if (clogLog.containsKey(bufferName)) {
            clogLog.get(bufferName).incrementAndGet();
        } else {
            clogLog.put(bufferName, new AtomicInteger(1));
        }
    }

    public static void logClog(String bufferName){
        trafficAnalyzer.logClogInternal(bufferName);
    }
}
