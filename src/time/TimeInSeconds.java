package time;

public class TimeInSeconds {
    private double value;

    public TimeInSeconds(double time) {
        value = time;
    }

    public double getValue() {
        return value;
    }

    public TimeInNanoSeconds toNanoSeconds() {
        return new TimeInNanoSeconds((long) (value*1000000000));
    }
}
