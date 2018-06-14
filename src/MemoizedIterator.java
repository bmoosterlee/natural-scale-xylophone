public class MemoizedIterator {

    final static FractionCalculator fractionCalculator = new FractionCalculator();
    int index;

    Fraction next() {
        Fraction nextHarmonicAsFraction = getFractionCalculator().getFraction(index);
        this.index = index + 1;
        return nextHarmonicAsFraction;
    }

    public FractionCalculator getFractionCalculator() {
        return fractionCalculator;
    }
}