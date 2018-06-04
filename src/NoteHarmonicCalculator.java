public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    Note note;
    Fraction currentFraction;
    RelativelyPrimeFractionIterator iterator;
    double noteVolume;

    public NoteHarmonicCalculator(Note note, double noteVolume){
        this.note = note;
        iterator = new RelativelyPrimeFractionIterator();
        currentFraction = getNextHarmonic();
        this.noteVolume = noteVolume;
    }

    public Harmonic poll(){
        Harmonic currentHarmonic = new Harmonic(note, currentFraction);
        currentHarmonic.noteVolume = noteVolume;

        currentFraction = getNextHarmonic();
        return currentHarmonic;
    }

    private Fraction getNextHarmonic() {
        return iterator.next();
    }

    public Harmonic peek(){
        Harmonic currentHarmonic = new Harmonic(note, currentFraction);
        currentHarmonic.noteVolume = noteVolume;
        return currentHarmonic;
    }

    @Override
    public int compareTo(NoteHarmonicCalculator o) {
        return peek().compareTo(o.peek());
    }
}
