package harmonics;

import java.util.Iterator;

class RelativelyPrimeFractionIterator implements Iterator<Fraction> {

    private final FractionIterator fractionIterator;

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

    private boolean isRelativelyPrime(Fraction fraction){
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
