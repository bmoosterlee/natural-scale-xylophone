public class MemoizedIterator {

    final static FractionMemoizer FRACTION_MEMOIZER = new FractionMemoizer();
    int index;

    Fraction next() {
        Fraction nextHarmonicAsFraction = FRACTION_MEMOIZER.getFraction(index);
        this.index = index + 1;
        return nextHarmonicAsFraction;
    }

}