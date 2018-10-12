package time;

public class TimeInNanoSeconds {
    private final long value;

    public TimeInNanoSeconds(long time) {
        this.value = time;
    }

    public static TimeInNanoSeconds now() {
        return new TimeInNanoSeconds(System.nanoTime());
    }

    public TimeInNanoSeconds subtract(TimeInNanoSeconds other) {
        return new TimeInNanoSeconds(value - other.value);
    }

    public long getValue() {
        return value;
    }

    public TimeInMilliSeconds toMilliSeconds() {
        return new TimeInMilliSeconds(value/1000000);
    }

    public long divide(TimeInNanoSeconds other) {
        return value/other.value;
    }

    public TimeInNanoSeconds divide(int x) {
        return new TimeInNanoSeconds(value/x);
    }

    public TimeInSeconds toSeconds() {
        return new TimeInSeconds(value/1000000000);
    }

    public TimeInNanoSeconds add(TimeInNanoSeconds other) {
        return new TimeInNanoSeconds(value + other.value);
    }

    public boolean lessThan(TimeInNanoSeconds other) {
        return value < other.value;
    }
}
