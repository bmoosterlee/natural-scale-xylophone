public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    Note note;
    Harmonic currentHarmonic;
    RelativelyPrimeFractionIterator iterator;
    double noteVolume;

    public NoteHarmonicCalculator(Note note, double noteVolume){
        this.note = note;
        iterator = new RelativelyPrimeFractionIterator();
        currentHarmonic = new Harmonic(note, iterator.next());
        this.noteVolume = noteVolume;
    }

    public Harmonic poll(){
        Harmonic tempCurrentHarmonic = currentHarmonic;
        tempCurrentHarmonic.noteVolume = noteVolume;
        currentHarmonic = new Harmonic(note, iterator.next());
        return tempCurrentHarmonic;
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
