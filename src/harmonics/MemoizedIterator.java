package harmonics;

import java.util.Iterator;

class MemoizedIterator implements Iterator<Fraction> {

    private final static FractionMemoizer FRACTION_MEMOIZER = new FractionMemoizer(new RelativelyPrimeFractionIterator());
    private int index;

    public boolean hasNext(){
        return true;
    }

    public Fraction next() {
        Fraction nextHarmonicAsFraction = FRACTION_MEMOIZER.getFraction(index);
        this.index = index + 1;
        return nextHarmonicAsFraction;
    }

}