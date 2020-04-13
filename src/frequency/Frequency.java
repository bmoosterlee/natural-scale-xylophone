package frequency;

public class Frequency {

    private final  double value;

    public Frequency(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public Frequency divideBy(double divisor) {
        return new Frequency(value/divisor);
    }

    public Frequency multiplyBy(double multiplier) {
        return new Frequency(value * multiplier);
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }

        if(!(o instanceof Frequency)){
            return false;
        }

        Frequency oo = (Frequency) o;

        return Double.compare(value, oo.value) == 0;
    }
}
