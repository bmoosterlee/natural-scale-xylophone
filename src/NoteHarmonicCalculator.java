public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    Note note;
    Harmonic currentHarmonic;
    RelativelyPrimeFractionIterator iterator;
    double noteVolume;

    public NoteHarmonicCalculator(Note note, double noteVolume){
        this.note = note;
        iterator = new RelativelyPrimeFractionIterator();
        currentHarmonic = getNextHarmonic();
        this.noteVolume = noteVolume;
    }

    public Harmonic poll(){
        Harmonic tempCurrentHarmonic = currentHarmonic;
        tempCurrentHarmonic.noteVolume = noteVolume;
        currentHarmonic = getNextHarmonic();
        return tempCurrentHarmonic;
    }

    private Harmonic getNextHarmonic() {
        return new Harmonic(note, iterator.next());
    }

    public Harmonic peek(){
        currentHarmonic.noteVolume = noteVolume;
        return currentHarmonic;
    }

    @Override
    public int compareTo(NoteHarmonicCalculator o) {
        return peek().compareTo(o.peek());
    }
}
