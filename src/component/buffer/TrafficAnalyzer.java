package component.buffer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TrafficAnalyzer {

    protected static TrafficAnalyzer trafficAnalyzer;

    private Map<String, AtomicInteger> clogLog;

    public TrafficAnalyzer(){
        clogLog = new ConcurrentHashMap<>();
        trafficAnalyzer = this;
        start();
    }

    private void start() {
        new Thread(() -> {
            while(true){
                List<Map.Entry<String, AtomicInteger>> clogLog = getClogLog();
                for (Map.Entry<String, AtomicInteger> aClogLog : clogLog) {
                    System.out.println(aClogLog);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private List<Map.Entry<String, AtomicInteger>> getClogLog(){
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
