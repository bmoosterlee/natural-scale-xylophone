
public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    private final FractionCalculator fractionCalculator;
    private Fraction currentHarmonicAsFraction;
    private int index;
    private final double noteVolume;

    public NoteHarmonicCalculator(double noteVolume, FractionCalculator fractionCalculator){
        this.fractionCalculator = fractionCalculator;
        this.noteVolume = noteVolume;

        setIndex(0);
        setCurrentHarmonicAsFraction(getNextHarmonicAsFraction());
    }

    public Fraction poll(){
        Fraction currentHarmonicAsFraction = getCurrentHarmonicAsFraction();

        setIndex(getIndex() + 1);
        setCurrentHarmonicAsFraction(getNextHarmonicAsFraction());
        return currentHarmonicAsFraction;
    }

    private Fraction getNextHarmonicAsFraction() {
        return fractionCalculator.getFraction(getIndex());
    }

    @Override
    public int compareTo(NoteHarmonicCalculator o) {
        return -Double.compare(Harmonic.getSonanceValue(getNoteVolume(), getCurrentHarmonicAsFraction()), Harmonic.getSonanceValue(o.getNoteVolume(), o.getCurrentHarmonicAsFraction()));
    }

    public Fraction getCurrentHarmonicAsFraction() {
        return currentHarmonicAsFraction;
    }

    public void setCurrentHarmonicAsFraction(Fraction currentHarmonicAsFraction) {
        this.currentHarmonicAsFraction = currentHarmonicAsFraction;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getNoteVolume() {
        return noteVolume;
    }

}
