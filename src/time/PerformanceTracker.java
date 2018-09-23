package time;

import java.util.concurrent.ConcurrentHashMap;

public class PerformanceTracker implements Runnable{

    static PerformanceTracker performanceTracker;
    ConcurrentHashMap<String, Long> stateTimes = new ConcurrentHashMap<>();

    public PerformanceTracker(){
        performanceTracker = this;
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
        long frameTime = 1000/1;
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

    public void tick(){
        System.out.println("------------");
        for(String stateName : performanceTracker.stateTimes.keySet()){
            long stateValue = performanceTracker.stateTimes.get(stateName);
            System.out.println(stateName + " took : " + String.valueOf(stateValue/1000000000.));
            performanceTracker.stateTimes.put(stateName, performanceTracker.stateTimes.get(stateName)-stateValue);
        }
    }
}
