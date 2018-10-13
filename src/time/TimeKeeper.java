package time;

public class TimeKeeper {

    private final String stateName;
    private long startTime;
    private long endTime;

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
