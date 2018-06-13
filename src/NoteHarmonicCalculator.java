import java.util.ArrayList;

public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    private Note note;
    private Fraction currentFraction;
    private int index;
    private ArrayList<Fraction> calculatedFractions;
    private int lastCalculatedIndex;
    private RelativelyPrimeFractionIterator iterator;
    private double noteVolume;

    public NoteHarmonicCalculator(Note note, double noteVolume){
        this.setNote(note);
        setIterator(new RelativelyPrimeFractionIterator());
        setCalculatedFractions(new ArrayList<>());
        setIndex(0);
        setLastCalculatedIndex(-1);
        setCurrentFraction(getNextHarmonic());
        this.setNoteVolume(noteVolume);
    }

    public Harmonic poll(){
        Harmonic currentHarmonic = new Harmonic(getNote(), getCurrentFraction());
        currentHarmonic.noteVolume = getNoteVolume();

        setIndex(getIndex() + 1);
        setCurrentFraction(getNextHarmonic());
        return currentHarmonic;
    }

    private Fraction getNextHarmonic() {
        if(getIndex() > getLastCalculatedIndex()){
            Fraction newFraction  = getIterator().next();
            getCalculatedFractions().add(newFraction);
            setLastCalculatedIndex(getLastCalculatedIndex() + 1);
            return newFraction;
        }
        else{
            return getCalculatedFractions().get(getIndex());
        }
    }

    public Harmonic peek(){
        Harmonic currentHarmonic = new Harmonic(getNote(), getCurrentFraction());
        currentHarmonic.noteVolume = getNoteVolume();
        return currentHarmonic;
    }

    @Override
    public int compareTo(NoteHarmonicCalculator o) {
        return peek().compareTo(o.peek());
    }

    public ArrayList<Fraction> getCalculatedFractions() {
        return calculatedFractions;
    }

    public void setCalculatedFractions(ArrayList<Fraction> calculatedFractions) {
        this.calculatedFractions = calculatedFractions;
    }

    public int getLastCalculatedIndex() {
        return lastCalculatedIndex;
    }

    public void setLastCalculatedIndex(int lastCalculatedIndex) {
        this.lastCalculatedIndex = lastCalculatedIndex;
    }

    public RelativelyPrimeFractionIterator getIterator() {
        return iterator;
    }

    public void setIterator(RelativelyPrimeFractionIterator iterator) {
        this.iterator = iterator;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public Fraction getCurrentFraction() {
        return currentFraction;
    }

    public void setCurrentFraction(Fraction currentFraction) {
        this.currentFraction = currentFraction;
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

    public void setNoteVolume(double noteVolume) {
        this.noteVolume = noteVolume;
    }
}
