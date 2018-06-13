import java.util.ArrayList;

public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    private final FractionCalculator fractionCalculator = new FractionCalculator();
    private Note note;
    private Fraction currentFraction;
    private int index;
    private double noteVolume;

    public NoteHarmonicCalculator(Note note, double noteVolume){
        this.setNote(note);
        setIndex(0);
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
        return fractionCalculator.getFraction(getIndex());
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
