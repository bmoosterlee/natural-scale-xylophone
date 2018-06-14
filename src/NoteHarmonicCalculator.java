
public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    private final FractionCalculator fractionCalculator;
    private final Note note;
    private Fraction currentHarmonicAsFraction;
    private int index;
    private final double noteVolume;

    public NoteHarmonicCalculator(Note note, double noteVolume, FractionCalculator fractionCalculator){
        this.note = note;
        this.fractionCalculator = fractionCalculator;
        this.noteVolume = noteVolume;
        
        setIndex(0);
        setCurrentHarmonicAsFraction(getNextHarmonicAsFraction());
    }

    public Harmonic poll(){
        Harmonic currentHarmonic = new Harmonic(getNote(), getCurrentHarmonicAsFraction());
        currentHarmonic.noteVolume = getNoteVolume();

        setIndex(getIndex() + 1);
        setCurrentHarmonicAsFraction(getNextHarmonicAsFraction());
        return currentHarmonic;
    }

    private Fraction getNextHarmonicAsFraction() {
        return fractionCalculator.getFraction(getIndex());
    }

    @Override
    public int compareTo(NoteHarmonicCalculator o) {
        Harmonic currentHarmonic = new Harmonic(getNote(), getCurrentHarmonicAsFraction());
        currentHarmonic.noteVolume = getNoteVolume();

        Harmonic otherCurrentHarmonic = new Harmonic(o.getNote(), o.getCurrentHarmonicAsFraction());
        otherCurrentHarmonic.noteVolume = o.getNoteVolume();
        return currentHarmonic.compareTo(otherCurrentHarmonic);
    }

    public Note getNote() {
        return note;
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
