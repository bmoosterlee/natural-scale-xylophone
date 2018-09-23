package time;

public class TimeInSeconds {
    private long value;

    public TimeInSeconds(long time) {
        value = time;
    }

    public long getValue() {
        return value;
    }

    public TimeInNanoSeconds toNanoSeconds() {
        return new TimeInNanoSeconds(value*1000000000);
    }
}
