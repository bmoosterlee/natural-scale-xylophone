package main;

public class TimeKeeper {

    String stateName;
    long startTime;
    long endTime;

    public TimeKeeper(String stateName){
        this.stateName = stateName;
    }

    public void start(){
        startTime = System.nanoTime();
    }

    public void stop(){
        endTime = System.nanoTime();
    }

    public String getStateName(){
        return stateName;
    }

    public long getTimePassed(){
        return endTime-startTime;
    }
}
