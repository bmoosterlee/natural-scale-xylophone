public class MemoizedIterator {

    final static FractionMemoizer FRACTION_MEMOIZER = new FractionMemoizer();
    int index;

    Fraction next() {
        Fraction nextHarmonicAsFraction = getFractionCalculator().getFraction(index);
        this.index = index + 1;
        return nextHarmonicAsFraction;
    }

    public FractionMemoizer getFractionCalculator() {
        return FRACTION_MEMOIZER;
    }
}