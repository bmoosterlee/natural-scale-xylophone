import java.util.Iterator;

public class FractionIterator implements Iterator<Fraction> {

    Fraction index;
    boolean reciprocal;

    public FractionIterator(){
        index = new Fraction(1,1);
        reciprocal = true;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Fraction next() {
        Fraction nextElement = index;

        index = new Fraction(index.denominator, index.numerator);
        reciprocal = !reciprocal;

        if(!reciprocal) {
            if (index.numerator < index.denominator) {
                index = new Fraction(index.numerator + 1, index.denominator);
            } else {
                index = new Fraction(1, index.denominator + 1);
            }
        }
        return nextElement;
    }

}
