package time;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceTracker implements Runnable{

    private static PerformanceTracker performanceTracker;
    private final ConcurrentHashMap<String, Long> stateTimes = new ConcurrentHashMap<>();

    public PerformanceTracker(){
        performanceTracker = this;
        start();
    }

    public static TimeKeeper startTracking(String stateName){
        TimeKeeper timeKeeper = new TimeKeeper(stateName);
        timeKeeper.start();
        return timeKeeper;
    }

    public static void stopTracking(TimeKeeper timeKeeper){
        timeKeeper.stop();
        if(!performanceTracker.stateTimes.containsKey(timeKeeper.getStateName())){
            performanceTracker.stateTimes.put(timeKeeper.getStateName(), timeKeeper.getTimePassed());
        }
        else {
            performanceTracker.stateTimes.put(timeKeeper.getStateName(), performanceTracker.stateTimes.get(timeKeeper.getStateName()) + timeKeeper.getTimePassed());
        }
    }

    public static void start(){
        Thread thread = new Thread(performanceTracker);
        thread.start();
    }

    @Override
    public void run() {
        long frameTime = 1000;
        long startTime;
        long currentTime;

        while(true) {
            startTime = System.nanoTime();
            tick();
            currentTime = System.nanoTime();

            long timePassed = (currentTime-startTime)/1000000;
            long waitTime = frameTime-timePassed;

            if(waitTime>0){
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void tick(){
        System.out.println("------------");
        ConcurrentHashMap.KeySetView<String, Long> names = performanceTracker.stateTimes.keySet();
        PriorityQueue<String> sortedNames = new PriorityQueue<>(Comparator.comparing(o -> -performanceTracker.stateTimes.get(o)));
        sortedNames.addAll(names);

        byte counter = 0x00;
        while(!sortedNames.isEmpty() && counter<5){
            String stateName = sortedNames.poll();
            long stateValue = performanceTracker.stateTimes.get(stateName);
            System.out.println(stateName + " took : " + String.valueOf(stateValue/1000000000.));
            performanceTracker.stateTimes.put(stateName, performanceTracker.stateTimes.get(stateName)-stateValue);

            counter++;
        }
    }
}
