package time;

import java.util.Objects;

public class TimeInSeconds {
    private double value;

    public TimeInSeconds(double time) {
        value = time;
    }

    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeInSeconds that = (TimeInSeconds) o;
        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    public TimeInNanoSeconds toNanoSeconds() {
        return new TimeInNanoSeconds((long) (value*1000000000));
    }
}
