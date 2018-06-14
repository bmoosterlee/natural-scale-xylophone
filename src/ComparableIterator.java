
public class ComparableIterator implements Comparable<ComparableIterator>{

    private final FractionCalculator fractionCalculator;
    private Fraction currentHarmonicAsFraction;
    private int index;
    private final double noteVolume;

    public ComparableIterator(double noteVolume, FractionCalculator fractionCalculator){
        this.fractionCalculator = fractionCalculator;
        this.noteVolume = noteVolume;

        this.index = 0;
        this.currentHarmonicAsFraction = getNextHarmonicAsFraction();
    }

    public Fraction poll(){
        Fraction currentHarmonicAsFraction = this.currentHarmonicAsFraction;

        this.index = index + 1;
        this.currentHarmonicAsFraction = getNextHarmonicAsFraction();
        return currentHarmonicAsFraction;
    }

    private Fraction getNextHarmonicAsFraction() {
        return fractionCalculator.getFraction(index);
    }

    @Override
    public int compareTo(ComparableIterator o) {
        return -Double.compare(Harmonic.getSonanceValue(noteVolume, currentHarmonicAsFraction), Harmonic.getSonanceValue(o.noteVolume, o.currentHarmonicAsFraction));
    }

}
