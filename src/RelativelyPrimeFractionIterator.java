import java.util.Iterator;

public class RelativelyPrimeFractionIterator implements Iterator<Fraction> {

    private FractionIterator fractionIterator;

    public RelativelyPrimeFractionIterator(){
        fractionIterator = new FractionIterator();
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Fraction next() {
        Fraction nextFraction = fractionIterator.next();
        while(!isRelativelyPrime(nextFraction)){
            nextFraction = fractionIterator.next();
        }
        return nextFraction;
    }

    public boolean isRelativelyPrime(Fraction fraction){
        return gcd(fraction.numerator, fraction.denominator) == 1;
    }

    private int gcd(int a, int b) {
        //Using euclid's algorithm where a and b are larger than zero.
        if(a>b){
            return gcd(a-b, b);
        }
        else if(a<b){
            return gcd(a, b-a);
        }
        else {
            return a;
        }
    }

}
