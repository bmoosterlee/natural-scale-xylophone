import java.util.Iterator;

public class MemoizedIterator implements Iterator<Fraction> {

    final static FractionMemoizer FRACTION_MEMOIZER = new FractionMemoizer();
    int index;

    public boolean hasNext(){
        return true;
    }

    public Fraction next() {
        Fraction nextHarmonicAsFraction = FRACTION_MEMOIZER.getFraction(index);
        this.index = index + 1;
        return nextHarmonicAsFraction;
    }

}